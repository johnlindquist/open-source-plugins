package com.johnlindquist.quickjump;

import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindResult;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.UsageInfo2UsageAdapter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * User: John Lindquist
 * Date: 9/8/11
 * Time: 12:10 AM
 */
public class QuickJumpAction extends AnAction{

    protected Project project;
    protected Editor editor;
    protected FindModel model;
    protected FindManager findManager;
    protected JBPopup popup;
    protected VirtualFile virtualFile;

    public void actionPerformed(AnActionEvent e){

        project = e.getData(PlatformDataKeys.PROJECT);
        editor = e.getData(PlatformDataKeys.EDITOR);
        virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);

        findManager = FindManager.getInstance(project);
        model = createFindModel(findManager);


        SearchBox searchBox = new SearchBox();
        searchBox.setSize(searchBox.getPreferredSize());

        ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(searchBox, null);
        popup = popupBuilder.createPopup();

        popup.show(guessBestLocation(editor));
        searchBox.requestFocus();
    }

    protected FindModel createFindModel(FindManager findManager){
        FindModel clone = (FindModel) findManager.getFindInFileModel().clone();
        clone.setFindAll(true);
        clone.setFromCursor(true);
        clone.setForward(true);
        clone.setRegularExpressions(false);

        return clone;
    }

    public RelativePoint guessBestLocation(Editor editor){
        VisualPosition logicalPosition = editor.getCaretModel().getVisualPosition();
        return getPointFromVisualPosition(editor, logicalPosition);
    }

    protected RelativePoint getPointFromVisualPosition(Editor editor, VisualPosition logicalPosition){
        Point p = editor.visualPositionToXY(new VisualPosition(logicalPosition.line + 1, logicalPosition.column));

        final Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
        if (!visibleArea.contains(p)){
            p = new Point((visibleArea.x + visibleArea.width) / 2, (visibleArea.y + visibleArea.height) / 2);
        }

        return new RelativePoint(editor.getContentComponent(), p);
    }


    private class SearchBox extends JTextField{
        private ArrayList<Balloon> balloons = new ArrayList<Balloon>();
        protected HashMap<Integer, UsageInfo> hashMap = new HashMap<Integer, UsageInfo>();
        protected int key;


        private SearchBox(){
            addKeyListener(new KeyAdapter(){
                @Override public void keyPressed(KeyEvent e){
                    System.out.println("released");
                    char keyChar = e.getKeyChar();
                    key = Character.getNumericValue(keyChar);

                    System.out.println(KeyEvent.VK_ENTER);
                    System.out.println(e.getKeyCode());
                    if (e.getKeyCode() == KeyEvent.VK_ENTER){
                        key = 0;

                    }
                }

                @Override public void keyTyped(KeyEvent e){
                    System.out.println("typed");
                    if (key >= 0 && key < 10 && !getText().equals("")){

                        UsageInfo usageInfo = hashMap.get(key);
                        if (usageInfo != null){
                            popup.cancel();
                            editor.getCaretModel().moveToOffset(usageInfo.getNavigationOffset());
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
            System.out.println(getText());
            model.setStringToFind(getText());
            java.util.List<UsageInfo> all = findAllVisible(project, editor, model);

            for (Balloon balloon1 : balloons){
                balloon1.dispose();
            }

            final int caretOffset = editor.getCaretModel().getOffset();
            RelativePoint caretPoint = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(caretOffset));
            final Point cP = caretPoint.getOriginalPoint();
            Collections.sort(all, new Comparator<UsageInfo>(){
                @Override public int compare(UsageInfo o1, UsageInfo o2){

                    RelativePoint o1Point = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(o1.getNavigationOffset()));
                    RelativePoint o2Point = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(o2.getNavigationOffset()));
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
                    else {
                        return -1;
                    }
                }
            });


            int size = all.size();
            if (size > 9){
                size = 9;
            }



            for (int i = 0; i < size; i++){
                UsageInfo usageInfo = all.get(i);

                int textOffset = usageInfo.getNavigationOffset();
                RelativePoint point = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(textOffset));
                point.getOriginalPoint().translate(0, -editor.getLineHeight() / 2);

                JPanel jPanel = new JPanel();
                jPanel.setBackground(Color.WHITE);
                int mnemoicNumber = i;
                String text = String.valueOf(mnemoicNumber);
                if (i == 0){
                    text = "Enter";
                }

                JLabel jLabel = new JLabel(text);
                Font jLabelFont = new Font(jLabel.getFont().getName(), Font.BOLD, 11);
                jLabel.setFont(jLabelFont);
                jLabel.setBackground(Color.LIGHT_GRAY);
                jLabel.setHorizontalAlignment(CENTER);
                jPanel.add(jLabel);

                BalloonBuilder balloonBuilder = JBPopupFactory.getInstance().createBalloonBuilder(jPanel);
                balloonBuilder.setFadeoutTime(0);
                balloonBuilder.setAnimationCycle(0);
                balloonBuilder.setHideOnClickOutside(true);
                balloonBuilder.setHideOnKeyOutside(true);
                balloonBuilder.setHideOnAction(true);
                balloonBuilder.setFillColor(new Color(0xdddddd));
                balloonBuilder.setBorderColor(new Color(0x888888));

                Balloon balloon = balloonBuilder.createBalloon();

                balloon.show(point, Balloon.Position.above);

                balloons.add(balloon);
                hashMap.put(mnemoicNumber, usageInfo);
            }
        }

        @Override public Dimension getPreferredSize(){
            return new Dimension(100, 20);
        }

        @Nullable
        protected java.util.List<UsageInfo> findAllVisible(final Project project, final Editor editor, final FindModel findModel){
            final Document document = editor.getDocument();
            final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
            if (psiFile == null){
                return null;
            }

            CharSequence text = document.getCharsSequence();
            int textLength = document.getTextLength();
            final java.util.List<UsageInfo> usages = new ArrayList<UsageInfo>();
            FindManager findManager = FindManager.getInstance(project);
            findModel.setForward(true); // when find all there is no diff in direction

            int offset = 0;

            Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();


            while (offset < textLength){
                FindResult result = findManager.findString(text, offset, findModel, virtualFile);
                if (!result.isStringFound()){
                    break;
                }


                UsageInfo2UsageAdapter usageAdapter = new UsageInfo2UsageAdapter(new UsageInfo(psiFile, result.getStartOffset(), result.getEndOffset()));
                Point point = editor.logicalPositionToXY(editor.offsetToLogicalPosition(usageAdapter.getUsageInfo().getNavigationOffset()));
                if (visibleArea.contains(point)){
                    UsageInfo usageInfo = usageAdapter.getUsageInfo();
                    usages.add(usageInfo);
                }


                final int prevOffset = offset;
                offset = result.getEndOffset();


                if (prevOffset == offset){
                    // for regular expr the size of the match could be zero -> could be infinite loop in finding usages!
                    ++offset;
                }
            }
            return usages;
        }
    }
}
