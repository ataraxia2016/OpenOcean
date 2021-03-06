package open.dolphin.client;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.StringTokenizer;
import javax.swing.Action;
import javax.swing.text.*;
import open.dolphin.infomodel.DocumentModel;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.ModuleModel;
import open.dolphin.infomodel.ProgressCourse;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * KarteRenderer_2
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 */
public class KarteRenderer_2 {
    
    private static final String COMPONENT_ELEMENT_NAME = "component";
    
    private static final String STAMP_HOLDER = "stampHolder";
    
    private static final String SCHEMA_HOLDER = "schemaHolder";
    
    private static final String ATTACHMENT_HOLDER = "attachmentHolder";
    
    private static final int TT_SECTION = 0;
    
    private static final int TT_PARAGRAPH = 1;
    
    private static final int TT_CONTENT = 2;
    
    private static final int TT_ICON = 3;
    
    private static final int TT_COMPONENT = 4;
    
    private static final int TT_PROGRESS_COURSE = 5;
    
    private static final String SECTION_NAME = "section";
    
    private static final String PARAGRAPH_NAME = "paragraph";
    
    private static final String CONTENT_NAME = "content";
    
    private static final String COMPONENT_NAME = "component";
    
    private static final String ICON_NAME = "icon";
    
    private static final String ALIGNMENT_NAME = "Alignment";
    
    private static final String FOREGROUND_NAME = "foreground";
    
    private static final String SIZE_NAME = "size";
    
    private static final String BOLD_NAME = "bold";
    
    private static final String ITALIC_NAME = "italic";
    
    private static final String UNDERLINE_NAME = "underline";
    
    private static final String TEXT_NAME = "text";
    
    private static final String NAME_NAME = "name";
    
    private static final String LOGICAL_STYLE_NAME = "logicalStyle";
    
    private static final String PROGRESS_COURSE_NAME = "kartePane";
    
    private static final String[] REPLACES = new String[] { "&", "<", ">", "'" ,"\""};
    
    private static final String[] MATCHES = new String[] { "&amp;", "&lt;", "&gt;", "&apos;", "&quot;" };
    
    private static final String NAME_STAMP_HOLDER = "name=\"stampHolder\"";
    
    private DocumentModel model;
    
    private final KartePane soaPane;
    
    private final KartePane pPane;
    
    private KartePane thePane;
    
    boolean logicalStyle;
    
    private boolean bSoaPane;
    
    private ArrayList<ModuleModel> soaModules;
    
    private ArrayList<ModuleModel> pModules;
    
    private boolean DEBUG;
    
    
    /** Creates a new instance of TextPaneRestoreBuilder
     * @param soaPane
     * @param pPane */
    public KarteRenderer_2(KartePane soaPane, KartePane pPane) {
        this.soaPane = soaPane;
        this.pPane = pPane;
    }
    
    /**
     * DocumentModel をレンダリングする。
     * @param model レンダリングする DocumentModel
     */
    public void render(DocumentModel model) {
        
        this.model = model;
        
        Collection<ModuleModel> modules = model.getModules();
        
        if (modules==null || modules.isEmpty()) {
            return;
        }       
        
        // SOA と P のモジュールをわける
        // また夫々の Pane の spec を取得する
        soaModules = new ArrayList<>();
        pModules = new ArrayList<>();
        String soaSpec = null;
        String pSpec = null;
        
        for (ModuleModel bean : modules) {
            
            String role = bean.getModuleInfoBean().getStampRole();
            
            if (role.equals(IInfoModel.ROLE_SOA)) {
                soaModules.add(bean);
                
            } else if (role.equals(IInfoModel.ROLE_SOA_SPEC)) {
                soaSpec = ((ProgressCourse) bean.getModel()).getFreeText();
                
            } else if (role.equals(IInfoModel.ROLE_P)) {
                pModules.add(bean);
                
            } else if (role.equals(IInfoModel.ROLE_P_SPEC)) {
                pSpec = ((ProgressCourse) bean.getModel()).getFreeText();
                
            }
            //-------------------------------------------------------
            // goody
            else if (bean.getModel() instanceof ProgressCourse) {
                if (soaSpec==null) {
                    soaSpec = ((ProgressCourse) bean.getModel()).getFreeText();
                } else if (pSpec==null) {
                    pSpec = ((ProgressCourse) bean.getModel()).getFreeText();
                }
                
            }else {
                pModules.add(bean);
            }
            //--------------------------------------------------------
        }
        
        Collections.sort(pModules);//これがなー
        
        if (soaSpec != null && pSpec != null) {
            
            int index = soaSpec.indexOf(NAME_STAMP_HOLDER);
            if (index > 0) {
                String sTmp = soaSpec;
                String pTmp = pSpec;
                soaSpec = pTmp;
                pSpec = sTmp;
            }
        }
        
        // SOA Pane をレンダリングする
        if (soaSpec == null || soaSpec.equals("")) {
            for (ModuleModel mm : soaModules) {
                soaPane.stamp(mm);
                soaPane.makeParagraph();
            }
            
        } else {
            debug("Render SOA Pane");
            debug("Module count = " + soaModules.size());
            bSoaPane = true;
            thePane = soaPane;
            renderPane(soaSpec);
        }
        
        // P Pane をレンダリングする
        if (pSpec == null || pSpec.equals("")) {
            // 前回処方適用のようにモジュールのみの場合
            for (ModuleModel mm : pModules) {
                //pPane.stamp(mm);
                pPane.flowStamp(mm);
                pPane.makeParagraph();
                pPane.makeParagraph();
            }
            
        } else {
            bSoaPane = false;
            thePane = pPane;
            renderPane(pSpec);
        }
    }
    
    /**
     * TextPane Dump の XML を解析する。
     * @param xml TextPane Dump の XML
     */
    private void renderPane(String xml) {
        
        debug(xml);
        
        SAXBuilder docBuilder = new SAXBuilder();
        
        try {
            StringReader sr = new StringReader(xml);
            Document doc = docBuilder.build(new BufferedReader(sr));
            org.jdom.Element root = (org.jdom.Element) doc.getRootElement();
            
            writeChildren(root);
        }
        // indicates a well-formedness error
        catch (JDOMException e) {
            //e.printStackTrace(System.err);
        } catch (IOException e) {
            //e.printStackTrace(System.err);
        }
    }
    
    /**
     * 子要素をパースする。
     * @param current 要素
     */
    private void writeChildren(org.jdom.Element current) {
        
        int eType = -1;
        String eName = current.getName();
        
        if (eName.equals(PARAGRAPH_NAME)) {
            eType = TT_PARAGRAPH;
            startParagraph(current.getAttributeValue(LOGICAL_STYLE_NAME),
                    current.getAttributeValue(ALIGNMENT_NAME));
            
        } else if (eName.equals(CONTENT_NAME) && (current.getChild(TEXT_NAME) != null)) {
            eType = TT_CONTENT;
            startContent(current.getAttributeValue(FOREGROUND_NAME), 
                    current.getAttributeValue(SIZE_NAME), 
                    current.getAttributeValue(BOLD_NAME), 
                    current.getAttributeValue(ITALIC_NAME), 
                    current.getAttributeValue(UNDERLINE_NAME), 
                    current.getChildText(TEXT_NAME));
            
        } else if (eName.equals(COMPONENT_NAME)) {
            eType = TT_COMPONENT;
            startComponent(current.getAttributeValue(NAME_NAME), // compoenet=number
                    current.getAttributeValue(COMPONENT_ELEMENT_NAME));
            
        } else if (eName.equals(ICON_NAME)) {
            eType = TT_ICON;
            startIcon(current);
            
        } else if (eName.equals(PROGRESS_COURSE_NAME)) {
            eType = TT_PROGRESS_COURSE;
            startProgressCourse();
            
        } else if (eName.equals(SECTION_NAME)) {
            eType = TT_SECTION;
            startSection();
            
        } else {
            debug("Other element:" + eName);
        }
        
        // 子を探索するのはパラグフとトップ要素のみ
        if (eType == TT_PARAGRAPH || eType == TT_PROGRESS_COURSE
                || eType == TT_SECTION) {
            
            java.util.List children = (java.util.List) current.getChildren();
            Iterator iterator = children.iterator();
            
            while (iterator.hasNext()) {
                org.jdom.Element child = (org.jdom.Element) iterator.next();
                writeChildren(child);
            }
        }
        
        switch (eType) {
            
            case TT_PARAGRAPH:
                endParagraph();
                break;
                
            case TT_CONTENT:
                endContent();
                break;
                
            case TT_ICON:
                endIcon();
                break;
                
            case TT_COMPONENT:
                endComponent();
                break;
                
            case TT_PROGRESS_COURSE:
                endProgressCourse();
                break;
                
            case TT_SECTION:
                endSection();
                break;
        }
    }
    
    private void startSection() {
    }
    
    private void endSection() {
    }
    
    private void startProgressCourse() {
    }
    
    private void endProgressCourse() {
    }
    
    private void startParagraph(String lStyle, String alignStr) {
        
        if (DEBUG) {
            debug("paragrap did start");
        }
        
        thePane.setLogicalStyle("default");
        logicalStyle = true;
        
        if (alignStr != null) {
            DefaultStyledDocument doc = (DefaultStyledDocument) thePane
                    .getTextPane().getDocument();
            Style style0 = doc.getStyle("default");
            Style style = doc.addStyle("alignment", style0);
            if (alignStr.equals("0")) {
                StyleConstants.setAlignment(style, StyleConstants.ALIGN_LEFT);
//s.oh^ 2014/04/02 修正時の中央揃え／右揃え対応
                if (thePane.getTextPane() != null) {
                    Action a = thePane.getTextPane().getActionMap().get("left-justify");
                    if (a != null) {
                        a.actionPerformed(new ActionEvent(thePane.getTextPane(), ActionEvent.ACTION_PERFORMED, null));
                    }
                }
//s.oh$
            } else if (alignStr.equals("1")) {
                StyleConstants.setAlignment(style, StyleConstants.ALIGN_CENTER);
//s.oh^ 2014/04/02 修正時の中央揃え／右揃え対応
                if (thePane.getTextPane() != null) {
                    Action a = thePane.getTextPane().getActionMap().get("center-justify");
                    if (a != null) {
                        a.actionPerformed(new ActionEvent(thePane.getTextPane(), ActionEvent.ACTION_PERFORMED, null));
                    }
                }        
//s.oh$
            } else if (alignStr.equals("2")) {
                StyleConstants.setAlignment(style, StyleConstants.ALIGN_RIGHT);
//s.oh^ 2014/04/02 修正時の中央揃え／右揃え対応
                if (thePane.getTextPane() != null) {
                    Action a = thePane.getTextPane().getActionMap().get("right-justify");
                    if (a != null) {
                        a.actionPerformed(new ActionEvent(thePane.getTextPane(), ActionEvent.ACTION_PERFORMED, null));
                    }
                }         
//s.oh$
            }
            thePane.setLogicalStyle("alignment");
            logicalStyle = true;
        }
    }
    
    private void endParagraph() {
        //thePane.makeParagraph(); // trim() の廃止で廃止
        if (DEBUG) {
            debug("paragrap did end");
        }
        if (logicalStyle) {
//s.oh^ 2014/04/02 修正時の中央揃え／右揃え対応
            if (thePane.getTextPane() != null) {
                Action a = thePane.getTextPane().getActionMap().get("left-justify");
                if (a != null) {
                    a.actionPerformed(new ActionEvent(thePane.getTextPane(), ActionEvent.ACTION_PERFORMED, null));
                }
            }
//s.oh$
            thePane.clearLogicalStyle();
            logicalStyle = false;
        }
    }
    
    private void startContent(String foreground, String size, String bold,
            String italic, String underline, String text) {
        
        if (DEBUG) {
            debug("content did start, text = " + text);
        }
        
        // 特殊文字を戻す
        for (int i = 0; i < REPLACES.length; i++) {
            text = text.replaceAll(MATCHES[i], REPLACES[i]);
        }
        
        // このコンテントに設定する AttributeSet
        MutableAttributeSet atts = new SimpleAttributeSet();
        
        // foreground 属性を設定する
        if (foreground != null) {
            StringTokenizer stk = new StringTokenizer(foreground, ",");
            if (stk.hasMoreTokens()) {
                int r = Integer.parseInt(stk.nextToken());
                int g = Integer.parseInt(stk.nextToken());
                int b = Integer.parseInt(stk.nextToken());
                StyleConstants.setForeground(atts, new Color(r, g, b));
            }
        }
        
        // size 属性を設定する
        if (size != null) {
            StyleConstants.setFontSize(atts, Integer.parseInt(size));
        }
        
        // bold 属性を設定する
        if (bold != null) {
            StyleConstants.setBold(atts, Boolean.valueOf(bold));
        }
        
        // italic 属性を設定する
        if (italic != null) {
            StyleConstants.setItalic(atts, Boolean.valueOf(italic));
        }
        
        // underline 属性を設定する
        if (underline != null) {
            StyleConstants.setUnderline(atts, Boolean.valueOf(underline));
        }
        
        // テキストを挿入する
        thePane.insertFreeString(text, atts);//ここで最終的にペーンに書き込む
    }
    
    private void endContent() {
    }
    
    private void startComponent(String name, String number) {
        
        if (DEBUG) {
            debug("Entering startComponent");
            debug("Name = " + name);
            debug("Number = " + number);
            debug("soaPane = " + bSoaPane);
        }
        
        try {
            if (name != null && name.equals(STAMP_HOLDER)) {
                int index = Integer.parseInt(number);
                ModuleModel stamp = bSoaPane
                        ? (ModuleModel) soaModules.get(index)
                        : (ModuleModel) pModules.get(index);
                thePane.flowStamp(stamp);
                
            } else if (name != null && name.equals(SCHEMA_HOLDER)) {
                int index = Integer.parseInt(number);
                thePane.flowSchema(model.getSchema(index));
                
            } else if (name != null && name.equals(ATTACHMENT_HOLDER)) {
                int index = Integer.parseInt(number);
                thePane.flowAttachment(model.getAttachment(index));
            }
            
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
    
    private void endComponent() {
    }
    
    private void startIcon(org.jdom.Element current) {
        
        String name = current.getChildTextTrim("name");
        
        if (name != null) {
            debug(name);
        }
    }
    
    private void endIcon() {
    }
    
    private void debug(String msg) {
        if (DEBUG) {
            java.util.logging.Logger.getLogger(this.getClass().getName()).info(msg);
        }
    }
}