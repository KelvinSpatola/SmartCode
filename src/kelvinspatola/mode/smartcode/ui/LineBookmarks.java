package kelvinspatola.mode.smartcode.ui;

import static kelvinspatola.mode.smartcode.Constants.PIN_MARKER;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import kelvinspatola.mode.smartcode.LinePainter;
import kelvinspatola.mode.smartcode.SmartCodeEditor;
import kelvinspatola.mode.smartcode.SmartCodeTextArea;
import processing.app.SketchCode;
import processing.mode.java.debug.LineHighlight;
import processing.mode.java.debug.LineID;


public class LineBookmarks implements LinePainter {
    private final List<LineMarker> markers = new ArrayList<>();
    private final List<BookmarkListListener> listeners = new ArrayList<>();
    private Color columnColor;
    protected SmartCodeEditor editor;
    
    public interface BookmarkListListener {
        void bookmarkAdded(Bookmark bm);
        void bookmarkRemoved(Bookmark bm);
    }
    
    
    // CONSTRUCTOR
    public LineBookmarks(SmartCodeEditor editor) {
        this.editor = editor;
        updateTheme();
    }

    
    /**
     * Adds a bookmark with a location assigned by a {@code LineID}.
     * 
     * @param lineID   the univocal line id for this bookmark.
     * @param colorTag the {@link ColorTag} reference for this marker.
     * @see <a href=
     *      "https://github.com/processing/processing4/blob/master/java/src/processing/mode/java/debug/LineID.java"
     *      target="_blank"> LineID</a>
     */
    public void addBookmark(LineID lineID, ColorTag colorTag) {
        Bookmark bm = new Bookmark(lineID, colorTag);
        markers.add(bm);
        markers.sort(null);
        listeners.stream().forEach(ls -> ls.bookmarkAdded(bm));
        editor.updateColumnPoints(markers, Bookmark.class);
    }

    
    /**
     * Removes a bookmark line with the provided {@code lineID}.
     * 
     * @param lineID the univocal line id for this bookmark.
     * @see <a href=
     *      "https://github.com/processing/processing4/blob/master/java/src/processing/mode/java/debug/LineID.java"
     *      target="_blank"> LineID</a>
     */
    public void removeBookmark(LineID lineID) {
        Bookmark bm = getBookmark(lineID);
        
        if (bm != null) {
            bm.dispose();
            markers.remove(bm);
            listeners.stream().forEach(ls -> ls.bookmarkRemoved(bm));
            editor.updateColumnPoints(markers, Bookmark.class);
        }
    }

    
    /**
     * Removes a bookmark line with the provided {@code marker}.
     * 
     * @param marker the marker to remove.
     * @throws IllegalArgumentException if the {@code marker} object is not of
     *                                  subtype {@link Bookmark}.
     * @see LineMarker
     */
    public void removeBookmark(LineMarker marker) {
        if (!(marker instanceof Bookmark)) {
            throw new IllegalArgumentException("Params for this method should be of subtype " + Bookmark.class);
        }
        Bookmark bm = (Bookmark) marker;
        
        if (bm != null) {
            bm.dispose();
            markers.remove(bm);
            listeners.stream().forEach(ls -> ls.bookmarkRemoved(bm));
            editor.updateColumnPoints(markers, Bookmark.class);
        }
    }

    
    /**
     * Checks whether there's a bookmark on the current tab with the line index
     * provided.
     * 
     * @param line the line index on the current tab
     * @return true if there's a bookmark on the current tab with this line index
     */
    public boolean isBookmark(int line) {
        final LineID lineID = editor.getLineIDInCurrentTab(line);
        return markers.stream().anyMatch(lm -> ((Bookmark) lm).isOnLine(lineID));
    }

    
    /**
     * Returns an existing bookmark with the provided {@link LineID}.
     * 
     * @param lineID The unique reference belonging to the bookmark
     * @return The bookmark with the provided {@link LineID} if this line is indeed
     *         bookmarked. Null otherwise.
     */
    public Bookmark getBookmark(LineID lineID) {
        return markers.stream()
                .map(Bookmark.class::cast)
                .filter(lm -> lm.isOnLine(lineID))
                .findAny()
                .orElse(null);
    }

    
    /**
     * Returns a list with all bookmarks in this sketch.
     * 
     * @return a list with all bookmarks as {@link LineMarker} instances.
     */
    public List<LineMarker> getMarkers() {
        return markers;
    }

    
    /**
     * Checks if there are bookmarks in the sketch.
     * 
     * @return true if exists at least one bookmark
     */
    public boolean hasBookmarks() {
        return !markers.isEmpty();
    }

    
    /**
     * Returns the number of bookmarks in this sketch.
     */
    public int markerCount() {
        return markers.size();
    }
    
    
    public void addBookmarkListListener(BookmarkListListener ls) {
        if (ls != null) {
            listeners.add(ls);
        }
    }
    
    public void stopBookmarkTracking() {
        markers.forEach(bm -> ((Bookmark) bm).stopTracking());
    }

    
    public void startBookmarkTracking() {
        markers.forEach(bm -> ((Bookmark) bm).startTracking());
    }

    
    @Override
    public boolean canPaint(Graphics gfx, int line, int y, int h, SmartCodeTextArea ta) {
        if (!SmartCodeTheme.BOOKMARKS_HIGHLIGHT || markers.isEmpty() || editor.isDebuggerEnabled())
            return false;

        if (isBookmark(line)) {
            Color color = getBookmark(editor.getLineIDInCurrentTab(line)).getColorTag().getColor();
            gfx.setColor(color);
            gfx.fillRect(0, y, editor.getWidth(), h);

            /*
             * In case this bookmarked line is part of a text selection or is the caret
             * line, it is necessary to paint it differently to give visual feedback to the
             * user. All the painting done to the text area by the SmartCode code is done
             * using the interface provided by the Processing source code, more precisely
             * the 'Highlight' interface inside the TextAreaPainter class. This makes all of
             * our painting happen strictly after the line highlight and selection highlight
             * paintings, overlapping and omitting them. To avoid that, we paint it
             * differently in order to give that feedback.
             */

            int selectionStartLine = ta.getSelectionStartLine();
            int selectionEndLine = ta.getSelectionStopLine();

            if (line >= selectionStartLine && line <= selectionEndLine) {
                int selectionStart = ta.getSelectionStart();
                int selectionEnd = ta.getSelectionStop();
                int lineStart = ta.getLineStartOffset(line);
                int x1, x2;

                if (selectionStart == selectionEnd) { // no selection
                    x1 = 0;
                    x2 = editor.getWidth();

                } else if (selectionStartLine == selectionEndLine) { // selection inside a line
                    x1 = ta._offsetToX(line, selectionStart - lineStart);
                    x2 = ta._offsetToX(line, selectionEnd - lineStart);

                } else if (line == selectionStartLine) { // block selection with caret at selection start
                    x1 = ta._offsetToX(line, selectionStart - lineStart);
                    x2 = editor.getWidth();

                } else if (line == selectionEndLine) { // block selection with caret at selection end
                    x1 = ta._offsetToX(line, 0);
                    x2 = ta._offsetToX(line, selectionEnd - lineStart);

                } else { // lines selected in the middle
                    x1 = ta._offsetToX(line, 0);
                    x2 = editor.getWidth();
                }

                final int dimming = 25;
                int rgb = color.getRGB();
                int r = Math.max(0, (rgb >> 16 & 0xFF) - dimming);
                int g = Math.max(0, (rgb >> 8 & 0xFF) - dimming);
                int b = Math.max(0, (rgb & 0xFF) - dimming);
                gfx.setColor(new Color(r, g, b));
                gfx.fillRect(Math.min(x1, x2), y, x1 > x2 ? (x1 - x2) : (x2 - x1), h);
            }
            return true;
        }
        return false;
    }

    
    @Override
    public void updateTheme() {
        short i = 1;
        for (ColorTag tag : ColorTag.values()) {
            tag.setColor(SmartCodeTheme.getColor("bookmarks.linehighlight.color." + (i++)));
        }
        columnColor = SmartCodeTheme.getColor("column.bookmark.color");
    }

    
    /**
     * This class represents a single marker. Each marker is unique. This class
     * wraps a <a href=
     * "https://github.com/processing/processing4/blob/master/java/src/processing/mode/java/debug/LineHighlight.java"
     * target="_blank"> LineHighlight</a> object which allows tracking changes to
     * the line number due to text edits, as well as wrapping a {@link ColorTag}
     * object in order to store the line's background color properties.
     * <p>
     * This class implements the {@code compareTo(Object)} method of the <a href=
     * "https://docs.oracle.com/javase/8/docs/api/java/lang/Comparable.html">
     * Comparable</a> interface to order each marker at the time of its creation.
     */
    public class Bookmark implements LineMarker, Comparable<Bookmark> {
        private LineHighlight highlight;
        private ColorTag colorTag;

        /**
         * Prevent this constructor from being called. Bookmarks must only be created
         * via the {@link LineBookmarks#addBookmark(LineID, ColorTag)} method
         */
        private Bookmark(LineID lineID, ColorTag colorTag) {
            highlight = new LineHighlight(lineID, editor);
            highlight.setMarker(PIN_MARKER);
            this.colorTag = colorTag;
        }

        public void paint() {
            highlight.paint();
        }

        @Override
        public int compareTo(Bookmark other) {
            if (this.getTabIndex() - other.getTabIndex() < 0)
                return -1;
            else if (this.getTabIndex() - other.getTabIndex() > 0)
                return 1;

            return this.getLine() - other.getLine();
        }

        public boolean isOnLine(LineID testLine) {
            return highlight.getLineID().equals(testLine);
        }

        public void stopTracking() {
            highlight.getLineID().stopTracking();
        }

        public void startTracking() {
            highlight.getLineID().startTracking(editor.currentDocument());
        }

        public void dispose() {
            highlight.clear();
            highlight.dispose();
        }

        public LineID getLineID() {
            return highlight.getLineID();
        }

        public void setColorTag(ColorTag colorTag) {
            this.colorTag = colorTag;
        }

        public ColorTag getColorTag() {
            return colorTag;
        }

        @Override
        public Class<?> getParent() {
            return this.getClass();
        }

        @Override
        public int getTabIndex() {
            SketchCode[] code = editor.getSketch().getCode();
            for (int i = 0; i < code.length; i++) {
                String tabName = code[i].getFileName();
                if (getLineID().fileName().equals(tabName))
                    return i;
            }
            return -1;
        }

        @Override
        public int getLine() {
            return highlight.getLineID().lineIdx();
        }

        @Override
        public int getStartOffset() {
            return editor.getTextArea().getLineStartOffset(getTabIndex(), getLine());
        }

        @Override
        public int getStopOffset() {
            return editor.getTextArea().getLineStopOffset(getTabIndex(), getLine()) - 1;
        }

        @Override
        public String getText() {
            String colorHex = Integer.toHexString(colorTag.getColor().getRGB()).substring(2);
            String lineText = editor.getTextArea().getLineText(getTabIndex(), getLine()).trim();
            String colorIndicator = "<font color=" + colorHex + "> &#x25A0; </font>"; // &#x25A0; -> HTML code for the
                                                                                      // square
            String lineNumberIndicator = "<font color=#bbbbbb>" + (getLine() + 1) + ": </font>";
            String lineTextIndicator = "<font color=#000000>" + lineText + "</font>";
            return "<html>" + colorIndicator + lineNumberIndicator + lineTextIndicator + "</html>";
        }
        
        @Override
        public String toString() {
            return getText();
        }

        @Override
        public void paintMarker(Graphics gfx, int x, int y, int w, int h) {
            gfx.setColor(columnColor);
            gfx.drawRect(x, y, w, h);
        }
    }
}
