package com.johnlindquist.quickjump;

import com.intellij.application.options.colors.ColorAndFontDescription;
import com.intellij.application.options.colors.ColorAndFontOptions;
import com.intellij.application.options.colors.FontOptions;
import com.intellij.application.options.editor.EditorOptions;
import com.intellij.application.options.editor.EditorOptionsProviderEP;
import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindResult;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actions.EditorActionUtil;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.colors.impl.EditorColorsManagerImpl;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.impl.FoldingModelImpl;
import com.intellij.openapi.editor.impl.ScrollingModelImpl;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.options.AbstractSchemesManager;
import com.intellij.openapi.options.SchemesManagerFactoryImpl;
import com.intellij.openapi.options.SchemesManagerImpl;
import com.intellij.openapi.options.newEditor.OptionsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.CodeStyleSchemes;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.psi.impl.cache.impl.id.IdTableBuilding;
import com.intellij.psi.impl.source.codeStyle.CodeStyleSchemeImpl;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.popup.AbstractPopup;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.BlockBorder;
import com.sun.awt.AWTUtilities;
import org.jdesktop.swingx.border.MatteBorderExt;
import org.jetbrains.annotations.Nullable;
import org.omg.CORBA.CompletionStatusHelper;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.AbstractBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.metal.MetalBorders;
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
public class QuickJumpAction extends AnAction {

    protected Project project;
    protected EditorImpl editor;
    protected FindModel findModel;
    protected FindManager findManager;
    protected AbstractPopup popup;
    protected VirtualFile virtualFile;
    protected DocumentImpl document;
    protected FoldingModelImpl foldingModel;
    protected SearchBox searchBox;
    protected DataContext dataContext;
    protected AnActionEvent inputEvent;
    private CaretModel caretModel;
    private Font font;
    private SelectionModel selectionModel;

    public void actionPerformed(AnActionEvent e) {
        inputEvent = e;

        project = e.getData(PlatformDataKeys.PROJECT);
        editor = (EditorImpl) e.getData(PlatformDataKeys.EDITOR);
        virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        document = (DocumentImpl) editor.getDocument();
        foldingModel = (FoldingModelImpl) editor.getFoldingModel();
        dataContext = e.getDataContext();
        caretModel = editor.getCaretModel();
        selectionModel = editor.getSelectionModel();

        findManager = FindManager.getInstance(project);
        findModel = createFindModel(findManager);

//        font = editor.getComponent().getFont();

        font = new Font("Arial", Font.BOLD, 11);
        searchBox = new SearchBox();

        searchBox.setFont(font);
        searchBox.setSize(searchBox.getPreferredSize());

        ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(searchBox, searchBox);
        popup = (AbstractPopup) popupBuilder.createPopup();

        popup.getContent().setBorder(new BlockBorder());

        popup.show(guessBestLocation(editor));
        popup.addListener(new JBPopupAdapter() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
                searchBox.hideBalloons();
            }
        });
        searchBox.requestFocus();

        searchBox.findText(0);
    }


    protected FindModel createFindModel(FindManager findManager) {
        FindModel clone = (FindModel) findManager.getFindInFileModel().clone();
        clone.setFindAll(true);
        clone.setFromCursor(true);
        clone.setForward(true);
        clone.setRegularExpressions(false);
        clone.setWholeWordsOnly(false);
        clone.setCaseSensitive(false);
        clone.setSearchHighlighters(true);
        clone.setPreserveCase(false);

        return clone;
    }

    public RelativePoint guessBestLocation(Editor editor) {
        VisualPosition logicalPosition = editor.getCaretModel().getVisualPosition();
        RelativePoint pointFromVisualPosition = getPointFromVisualPosition(editor, logicalPosition);
        pointFromVisualPosition.getOriginalPoint().translate(-4, -searchBox.getHeight());
        return pointFromVisualPosition;
    }

    protected RelativePoint getPointFromVisualPosition(Editor editor, VisualPosition logicalPosition) {
        Point p = editor.visualPositionToXY(new VisualPosition(logicalPosition.line + 1, logicalPosition.column));

        final Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
        if (!visibleArea.contains(p)) {
            p = new Point((visibleArea.x + visibleArea.width) / 2, (visibleArea.y + visibleArea.height) / 2);
        }

        return new RelativePoint(editor.getContentComponent(), p);
    }

    protected void moveCaret(Integer offset) {
        searchBox.cancelFindText();
        popup.cancel();
        editor.getSelectionModel().removeSelection();
        editor.getCaretModel().moveToOffset(offset);
//        editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
    }

    protected class SearchBox extends JTextField {
        private static final int ALLOWED_RESULTS = 10;
        private ArrayList<Balloon> balloons = new ArrayList<Balloon>();
        protected HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
        protected int key;
        protected Timer timer;
        protected List<Integer> results;
        protected int startResult;
        protected int endResult;

        private SearchBox() {

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    char keyChar = e.getKeyChar();
                    key = Character.getNumericValue(keyChar);

                    if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isControlDown() && e.isShiftDown()) {
                        startResult -= ALLOWED_RESULTS;
                        endResult -= ALLOWED_RESULTS;
                        if (startResult < 0) {
                            startResult = 0;
                        }
                        if (endResult < ALLOWED_RESULTS) {
                            endResult = ALLOWED_RESULTS;
                        }
                        showBalloons(results, startResult, endResult);
                    } else if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isControlDown()) {
                        startResult += ALLOWED_RESULTS;
                        endResult += ALLOWED_RESULTS;
                        showBalloons(results, startResult, endResult);
                    } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        key = 0;
                        if (getText().length() == 1) {
                            startSingleCharSearch();
                        }
                    }
                }

                @Override
                public void keyTyped(KeyEvent e) {
                    checkKeyAndMove();
                }
            });

            getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    startFindText();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    startFindText();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                }
            });

        }

        private void checkKeyAndMove() {

            if (key >= 0 && key < 10) {
                final Integer offset = hashMap.get(key);
                if (offset != null) {
                    popup.cancel();
                    moveCaret(offset);
                }
            }
        }

        private void startSingleCharSearch() {
            findText(0);
        }

        private void startFindText() {

            int delay = 100;

            String text = getText();
            int length = text.length();

            int width = 11 + getFontMetrics(getFont()).stringWidth(getText());
            int height = getHeight();
            popup.setSize(new Dimension(width, height));
            setSize(width, height);
//            System.out.println("the single char is: " + text);

            if (length == 0) {
                return;
            }

            if (length == 1) {
                char c = text.charAt(0);
                if (!Character.isDigit(c) && !Character.isLetter(c)) {
                    findText(delay);
                }
                return;
            }

            if (length < 2) {
                cancelFindText();
                return;
            }

            if (length == 2) {
                delay = 250;
            }

            findText(delay);
        }

        private void findText(int delay) {

            cancelFindText();

            timer = new Timer(delay, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                    String text = getText();
                    findModel.setRegularExpressions(false);
                    if (text.equals("")) {
                        text = document.getText(new TextRange(getWordAtCaretStart(), getWordAtCaretEnd()));
                    }
                    if (text.equals("")) {
                        return;
                    }

                    System.out.println(text);
                    findModel.setStringToFind(text);

                    ApplicationManager.getApplication().runReadAction(new Runnable() {
                        @Override
                        public void run() {
                            results = findAllVisible();

                            //camelCase logic
                            findCamelCase();
                        }

                    });
                    //clear duplicates (optimize?)
                    HashSet hashSet = new HashSet();
                    hashSet.addAll(results);
                    results.clear();
                    results.addAll(hashSet);

                    final int caretOffset = editor.getCaretModel().getOffset();
                    RelativePoint caretPoint = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(caretOffset));
                    final Point cP = caretPoint.getOriginalPoint();
                    Collections.sort(results, new Comparator<Integer>() {
                        @Override
                        public int compare(Integer o1, Integer o2) {

//                            RelativePoint o1Point = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(o1));
//                            RelativePoint o2Point = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(o2));
//                            Point o1P = o1Point.getOriginalPoint();
//                            Point o2P = o2Point.getOriginalPoint();
//
//                            double i1 = Point.distance(o1P.x, o1P.y, cP.x, cP.y);
//                            double i2 = Point.distance(o2P.x, o2P.y, cP.x, cP.y);
                            int i1 = Math.abs(caretOffset - o1);
                            int i2 = Math.abs(caretOffset - o2);
                            if (i1 > i2) {
                                return 1;
                            } else if (i1 == i2) {
                                return 0;
                            } else {
                                return -1;
                            }
                        }
                    });


                    startResult = 0;
                    endResult = ALLOWED_RESULTS;

                    showBalloons(results, startResult, endResult);

                }
            });

            timer.setRepeats(false);
            timer.start();

        }

        private void findCamelCase() {
            String text = getText();
            if (text.length() < 2) return;
            String[] strings = calcWords(text, editor);
            for (String string : strings) {
                findModel.setStringToFind(string);
                results.addAll(findAllVisible());
            }
        }

        private void cancelFindText() {
            //TODO: Fix edge case: searching for a 2 char, hitting delete, then searching for a 1 char and hitting Enter.
//            if(results != null) results.clear();
//            if(hashMap != null) hashMap.clear();
            if (timer != null) {
                timer.stop();
                timer = null;
            }
        }

        private void showBalloons(List<Integer> results, int start, int end) {
            hideBalloons();


            int size = results.size();
            if (end > size) {
                end = size;
            }


            final HashMap<Balloon, RelativePoint> balloonPointHashMap = new HashMap<Balloon, RelativePoint>();
            for (int i = start; i < end; i++) {

                int textOffset = results.get(i);
                RelativePoint point = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(textOffset));
                point.getOriginalPoint().translate(0, -editor.getLineHeight() / 2);

                JPanel jPanel = new JPanel(new GridLayout());
                jPanel.setBackground(new Color(255, 255, 255));
                int mnemoicNumber = i % ALLOWED_RESULTS;
                String text = String.valueOf(mnemoicNumber);
                if (i % ALLOWED_RESULTS == 0) {
                    text = "Enter";
                }

                JLabel jLabel = new JLabel(text);
//                Font jLabelFont = new Font(jLabel.getFont().getName(), Font.BOLD, 11);
                jLabel.setFont(font);
                jLabel.setBackground(new Color(192, 192, 192));
                jLabel.setHorizontalAlignment(CENTER);
                jLabel.setFocusable(false);
                jLabel.setSize(jLabel.getWidth(), 5);
                jPanel.setFocusable(false);
                jPanel.add(jLabel);

                if (text.equals("Enter")) {
                    jPanel.setPreferredSize(new Dimension(45, 13));

                } else {
                    jPanel.setPreferredSize(new Dimension(19, 13));

                }

                BalloonBuilder balloonBuilder = JBPopupFactory.getInstance().createBalloonBuilder(jPanel);
                balloonBuilder.setFadeoutTime(0);
                balloonBuilder.setAnimationCycle(0);
                balloonBuilder.setHideOnClickOutside(true);
                balloonBuilder.setHideOnKeyOutside(true);
                balloonBuilder.setHideOnAction(true);
                balloonBuilder.setFillColor(new Color(221, 221, 221));
                balloonBuilder.setBorderColor(new Color(136, 136, 136));

                Balloon balloon = balloonBuilder.createBalloon();
                balloonPointHashMap.put(balloon, point);


                balloons.add(balloon);
                hashMap.put(mnemoicNumber, textOffset);
            }

            Collections.sort(balloons, new Comparator<Balloon>() {
                @Override
                public int compare(Balloon o1, Balloon o2) {
                    RelativePoint point1 = balloonPointHashMap.get(o1);
                    RelativePoint point2 = balloonPointHashMap.get(o2);

                    if (point1.getOriginalPoint().y < point2.getOriginalPoint().y) {
                        return 1;
                    } else if (point1.getOriginalPoint().y == point2.getOriginalPoint().y) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
            });

            for (int i = 0, balloonsSize = balloons.size(); i < balloonsSize; i++) {
                Balloon balloon = balloons.get(i);
                RelativePoint point = balloonPointHashMap.get(balloon);
                balloon.show(point, Balloon.Position.above);
            }


        }

        private void hideBalloons() {
            for (Balloon balloon1 : balloons) {
                balloon1.dispose();
            }
            balloons.clear();
            hashMap.clear();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(20, 20);
        }

        @Nullable
        protected java.util.List<Integer> findAllVisible() {
            final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
            if (psiFile == null) {
                return null;
            }

            CharSequence text = document.getCharsSequence();
            final List<Integer> offsets = new ArrayList<Integer>();

            JViewport viewport = editor.getScrollPane().getViewport();
            double linesAbove = viewport.getViewPosition().getY() / editor.getLineHeight();

            ScrollingModelImpl scrollingModel = (ScrollingModelImpl) editor.getScrollingModel();
            Rectangle visibleArea = scrollingModel.getVisibleArea();

            double visibleLines = visibleArea.getHeight() / editor.getLineHeight() + 4;

            int offset = 0;
            int endOffset = 0;
            offset = document.getLineStartOffset((int) linesAbove);
            int endLine = (int) (linesAbove + visibleLines);
            int lineCount = document.getLineCount() - 1;
            if (endLine > lineCount) {
                endLine = lineCount;
            }
            endOffset = document.getLineEndOffset(endLine);

            while (offset < endOffset) {

                FindResult result = findManager.findString(text, offset, findModel, virtualFile);
                if (!result.isStringFound()) {
                    break;
                }

//                System.out.println("result: " + result.toString());

                UsageInfo2UsageAdapter usageAdapter = new UsageInfo2UsageAdapter(new UsageInfo(psiFile, result.getStartOffset(), result.getEndOffset()));
                Point point = editor.logicalPositionToXY(editor.offsetToLogicalPosition(usageAdapter.getUsageInfo().getNavigationOffset()));
                if (visibleArea.contains(point)) {
                    UsageInfo usageInfo = usageAdapter.getUsageInfo();
                    int navigationOffset = usageInfo.getNavigationOffset();
                    if (navigationOffset != caretModel.getOffset()) {
                        offsets.add(navigationOffset);
                    }
                }


                final int prevOffset = offset;
                offset = result.getEndOffset();


                if (prevOffset == offset) {
                    ++offset;
                }
            }

            return offsets;
        }
    }


    protected String[] calcWords(final String prefix, Editor editor) {
        final NameUtil.MinusculeMatcher matcher = (NameUtil.MinusculeMatcher) NameUtil.buildMatcher(prefix, 0, true, true);
        final Set<String> words = new HashSet<String>();
        CharSequence chars = editor.getDocument().getCharsSequence();

        IdTableBuilding.scanWords(new IdTableBuilding.ScanWordProcessor() {
            public void run(final CharSequence chars, final int start, final int end) {
                final String word = chars.subSequence(start, end).toString();
                if (matcher.matches(word)) {
                    System.out.println("word: " + word);
                    words.add(word);
                }
            }
        }, chars, 0, chars.length());

        ArrayList<String> sortedWords = new ArrayList<String>(words);
        Collections.sort(sortedWords);

        return ArrayUtil.toStringArray(sortedWords);
    }

    int getWordAtCaretStart() {
        Document document = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();
        if (offset == 0) return 0;
        int lineNumber = editor.getCaretModel().getLogicalPosition().line;
        CharSequence text = document.getCharsSequence();
        int newOffset = offset;
        int minOffset = lineNumber > 0 ? document.getLineEndOffset(lineNumber - 1) : 0;
        boolean camel = editor.getSettings().isCamelWords();
        for (; newOffset > minOffset; newOffset--) {
            if (EditorActionUtil.isWordStart(text, newOffset, camel)) break;
        }

        return newOffset;
    }

    int getWordAtCaretEnd() {
        Document document = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();

        CharSequence text = document.getCharsSequence();
        if (offset >= document.getTextLength() - 1 || document.getLineCount() == 0) return offset;

        int newOffset = offset + 1;

        int lineNumber = editor.getCaretModel().getLogicalPosition().line;
        int maxOffset = document.getLineEndOffset(lineNumber);
        if (newOffset > maxOffset) {
            if (lineNumber + 1 >= document.getLineCount()) return offset;
            maxOffset = document.getLineEndOffset(lineNumber + 1);
        }
        boolean camel = editor.getSettings().isCamelWords();
        for (; newOffset < maxOffset; newOffset++) {
            if (EditorActionUtil.isWordEnd(text, newOffset, camel)) break;
        }

        return newOffset;
    }

}
