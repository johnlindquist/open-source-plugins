package com.johnlindquist.quickjump;

import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindResult;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.impl.FoldingModelImpl;
import com.intellij.openapi.editor.impl.ScrollingModelImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.psi.impl.cache.impl.id.IdTableBuilding;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

/**
 * User: John Lindquist
 * Date: 9/8/11
 * Time: 12:10 AM
 */
public class QuickJumpAction extends AnAction{

    protected Project project;
    protected EditorImpl editor;
    protected FindModel findModel;
    protected FindManager findManager;
    protected JBPopup popup;
    protected VirtualFile virtualFile;
    protected DocumentImpl document;
    protected FoldingModelImpl foldingModel;
    protected SearchBox searchBox;
    protected DataContext dataContext;

    public void actionPerformed(AnActionEvent e){

        project = e.getData(PlatformDataKeys.PROJECT);
        editor = (EditorImpl) e.getData(PlatformDataKeys.EDITOR);
        virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        document = (DocumentImpl) editor.getDocument();
        foldingModel = (FoldingModelImpl) editor.getFoldingModel();
        dataContext = e.getDataContext();


        findManager = FindManager.getInstance(project);
        findModel = createFindModel(findManager);


        searchBox = new SearchBox();
        searchBox.setSize(searchBox.getPreferredSize());

        ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(searchBox, searchBox);
        popup = popupBuilder.createPopup();

        popup.show(guessBestLocation(editor));
        popup.addListener(new JBPopupAdapter(){
            @Override public void onClosed(LightweightWindowEvent event){
                searchBox.hideBalloons();
            }
        });
        searchBox.grabFocus();
    }

    protected FindModel createFindModel(FindManager findManager){
        FindModel clone = (FindModel) findManager.getFindInFileModel().clone();
        clone.setFindAll(true);
        clone.setFromCursor(true);
        clone.setForward(true);
        clone.setRegularExpressions(false);
        clone.setWholeWordsOnly(false);
        clone.setCaseSensitive(false);
        clone.setSearchHighlighters(true);

        return clone;
    }

    public RelativePoint guessBestLocation(Editor editor){
        VisualPosition logicalPosition = editor.getCaretModel().getVisualPosition();
        RelativePoint pointFromVisualPosition = getPointFromVisualPosition(editor, logicalPosition);
        pointFromVisualPosition.getOriginalPoint().translate(0, -editor.getLineHeight());
        return pointFromVisualPosition;
    }

    protected RelativePoint getPointFromVisualPosition(Editor editor, VisualPosition logicalPosition){
        Point p = editor.visualPositionToXY(new VisualPosition(logicalPosition.line + 1, logicalPosition.column));

        final Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
        if (!visibleArea.contains(p)){
            p = new Point((visibleArea.x + visibleArea.width) / 2, (visibleArea.y + visibleArea.height) / 2);
        }

        return new RelativePoint(editor.getContentComponent(), p);
    }

    protected void moveCaret(Integer offset){
        editor.getSelectionModel().removeSelection();
        editor.getCaretModel().moveToOffset(offset);
        editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
    }

    protected class SearchBox extends JTextField{
        private static final int ALLOWED_RESULTS = 10;
        private ArrayList<Balloon> balloons = new ArrayList<Balloon>();
        protected HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
        protected int key;
        protected Timer timer;
        protected List<Integer> results;
        protected int startResult;
        protected int endResult;


        private SearchBox(){
            addKeyListener(new KeyAdapter(){
                @Override public void keyPressed(KeyEvent e){
                    char keyChar = e.getKeyChar();
                    key = Character.getNumericValue(keyChar);

                    if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isControlDown() && e.isShiftDown()){
                        startResult -= ALLOWED_RESULTS;
                        endResult -= ALLOWED_RESULTS;
                        if (startResult < 0){
                            startResult = 0;
                        }
                        if (endResult < ALLOWED_RESULTS){
                            endResult = ALLOWED_RESULTS;
                        }
                        showBalloons(results, startResult, endResult);
                    }
                    else if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isControlDown()){
                        startResult += ALLOWED_RESULTS;
                        endResult += ALLOWED_RESULTS;
                        showBalloons(results, startResult, endResult);
                    }
                    else if (e.getKeyCode() == KeyEvent.VK_ENTER){
                        key = 0;
                    }
                }

                @Override public void keyTyped(KeyEvent e){
                    if (key >= 0 && key < 10 && !getText().equals("")){

                        final Integer offset = hashMap.get(key);
                        if (offset != null){

                            popup.cancel();
                            moveCaret(offset);

                        }
                    }


                }
            });

            getDocument().addDocumentListener(new DocumentListener(){
                @Override public void insertUpdate(DocumentEvent e){
                    findText();
                }

                @Override public void removeUpdate(DocumentEvent e){
                    findText();
                }

                @Override public void changedUpdate(DocumentEvent e){
                }
            });

        }

        private void findText(){
            final int length = getText().length();
            if (length < 2){
                return;
            }
            if (timer != null){
                timer.stop();
                timer = null;
            }
            int delay = 100;
            if (length == 2){
                delay = 250;
            }

            timer = new Timer(delay, new ActionListener(){
                @Override public void actionPerformed(ActionEvent e){
                    if (getText().length() < 2){
                        return;
                    }
                    ApplicationManager.getApplication().runReadAction(new Runnable(){
                        @Override
                        public void run(){
                            System.out.println(getText());
                            findModel.setStringToFind(getText());
                            results = findAllVisible();

                            //camelCase logic
                            String[] strings = calcWords(getText(), editor);
                            for (String string : strings){
                                findModel.setStringToFind(string);
                                results.addAll(findAllVisible());
                            }

                            //clear duplicates (optimize?)
                            HashSet hashSet = new HashSet();
                            hashSet.addAll(results);
                            results.clear();
                            results.addAll(hashSet);

                            final int caretOffset = editor.getCaretModel().getOffset();
                            RelativePoint caretPoint = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(caretOffset));
                            final Point cP = caretPoint.getOriginalPoint();
                            Collections.sort(results, new Comparator<Integer>(){
                                @Override public int compare(Integer o1, Integer o2){

                                    RelativePoint o1Point = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(o1));
                                    RelativePoint o2Point = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(o2));
                                    Point o1P = o1Point.getOriginalPoint();
                                    Point o2P = o2Point.getOriginalPoint();

                                    double i1 = Point.distance(o1P.x, o1P.y, cP.x, cP.y);
                                    double i2 = Point.distance(o2P.x, o2P.y, cP.x, cP.y);
                                    if (i1 > i2){
                                        return 1;
                                    }
                                    else if (i1 == i2){

                                        return 0;
                                    }
                                    else{
                                        return -1;
                                    }
                                }
                            });


                            startResult = 0;
                            endResult = ALLOWED_RESULTS;

                            showBalloons(results, startResult, endResult);
                        }
                    });
                }
            });

            timer.setRepeats(false);
            timer.start();

        }

        private void showBalloons(List<Integer> results, int start, int end){
            hideBalloons();


            int size = results.size();
            if (end > size){
                end = size;
            }


            final HashMap<Balloon, RelativePoint> balloonPointHashMap = new HashMap<Balloon, RelativePoint>();
            for (int i = start; i < end; i++){

                int textOffset = results.get(i);
                RelativePoint point = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(textOffset));
                point.getOriginalPoint().translate(0, -editor.getLineHeight() / 2);

                JPanel jPanel = new JPanel(new GridLayout());
                jPanel.setBackground(Color.WHITE);
                int mnemoicNumber = i % ALLOWED_RESULTS;
                String text = String.valueOf(mnemoicNumber);
                if (i % ALLOWED_RESULTS == 0){
                    text = "Enter";
                }

                JLabel jLabel = new JLabel(text);
                Font jLabelFont = new Font(jLabel.getFont().getName(), Font.BOLD, 11);
                jLabel.setFont(jLabelFont);
                jLabel.setBackground(Color.LIGHT_GRAY);
                jLabel.setHorizontalAlignment(CENTER);
                jLabel.setFocusable(false);
                jLabel.setSize(jLabel.getWidth(), 5);
                jPanel.setFocusable(false);
                jPanel.add(jLabel);

                if (text.equals("Enter")){
                    jPanel.setPreferredSize(new Dimension(45, 13));

                }else {
                    jPanel.setPreferredSize(new Dimension(19, 13));

                }

                BalloonBuilder balloonBuilder = JBPopupFactory.getInstance().createBalloonBuilder(jPanel);
                balloonBuilder.setFadeoutTime(0);
                balloonBuilder.setAnimationCycle(0);
                balloonBuilder.setHideOnClickOutside(true);
                balloonBuilder.setHideOnKeyOutside(true);
                balloonBuilder.setHideOnAction(true);
                balloonBuilder.setFillColor(new Color(0xdddddd));
                balloonBuilder.setBorderColor(new Color(0x888888));

                Balloon balloon = balloonBuilder.createBalloon();

                balloonPointHashMap.put(balloon, point);

                balloons.add(balloon);
                hashMap.put(mnemoicNumber, textOffset);
            }

            Collections.sort(balloons, new Comparator<Balloon>(){
                @Override public int compare(Balloon o1, Balloon o2){
                    RelativePoint point1 = balloonPointHashMap.get(o1);
                    RelativePoint point2 = balloonPointHashMap.get(o2);

                    if (point1.getOriginalPoint().y < point2.getOriginalPoint().y){
                        return 1;
                    }
                    else if (point1.getOriginalPoint().y == point2.getOriginalPoint().y){
                        return 0;
                    }
                    else{
                        return -1;
                    }
                }
            });

            for (int i = 0, balloonsSize = balloons.size(); i < balloonsSize; i++){
                Balloon balloon = balloons.get(i);
                RelativePoint point = balloonPointHashMap.get(balloon);
                balloon.show(point, Balloon.Position.above);
            }


        }

        private void hideBalloons(){
            for (Balloon balloon1 : balloons){
                balloon1.dispose();
            }
            balloons.clear();
            hashMap.clear();
        }

        @Override public Dimension getPreferredSize(){
            return new Dimension(100, 20);
        }

        @Nullable
        protected java.util.List<Integer> findAllVisible(){
            final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
            if (psiFile == null){
                return null;
            }

            CharSequence text = document.getCharsSequence();
            final List<Integer> usages = new ArrayList<Integer>();

            JViewport viewport = editor.getScrollPane().getViewport();
            double linesAbove = viewport.getViewPosition().getY() / editor.getLineHeight();

            ScrollingModelImpl scrollingModel = (ScrollingModelImpl) editor.getScrollingModel();
            Rectangle visibleArea = scrollingModel.getVisibleArea();

            double visibleLines = visibleArea.getHeight() / editor.getLineHeight() + 4;

            int offset = document.getLineStartOffset((int) linesAbove);
            int endLine = (int) (linesAbove + visibleLines);
            int lineCount = document.getLineCount() - 1;
            if (endLine > lineCount){
                endLine = lineCount;
            }
            int endOffset = document.getLineEndOffset(endLine);

            while (offset < endOffset){
                FindResult result = findManager.findString(text, offset, findModel, virtualFile);
                if (!result.isStringFound()){
                    break;
                }


                UsageInfo2UsageAdapter usageAdapter = new UsageInfo2UsageAdapter(new UsageInfo(psiFile, result.getStartOffset(), result.getEndOffset()));
                Point point = editor.logicalPositionToXY(editor.offsetToLogicalPosition(usageAdapter.getUsageInfo().getNavigationOffset()));
                if (visibleArea.contains(point)){
                    UsageInfo usageInfo = usageAdapter.getUsageInfo();
                    usages.add(usageInfo.getNavigationOffset());
                }


                final int prevOffset = offset;
                offset = result.getEndOffset();


                if (prevOffset == offset){
                    ++offset;
                }
            }
            return usages;
        }

        protected int getVisualLineCount(FoldingModelImpl foldingModel){
            return document.getLineCount() - foldingModel.getFoldedLinesCountBefore(document.getTextLength() + 1) + editor.getSoftWrapModel().getSoftWrapsIntroducedLinesNumber();
        }
    }

    protected String[] calcWords(final String prefix, Editor editor){
        final NameUtil.Matcher matcher = NameUtil.buildMatcher(prefix, 0, true, true);
        final Set<String> words = new HashSet<String>();
        CharSequence chars = editor.getDocument().getCharsSequence();

        IdTableBuilding.scanWords(new IdTableBuilding.ScanWordProcessor(){
            public void run(final CharSequence chars, final int start, final int end){
                final String word = chars.subSequence(start, end).toString();
                if (matcher.matches(word)){
                    words.add(word);
                }
            }
        }, chars, 0, chars.length());


        ArrayList<String> sortedWords = new ArrayList<String>(words);
        Collections.sort(sortedWords);

        return ArrayUtil.toStringArray(sortedWords);
    }

}
