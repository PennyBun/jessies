package terminatorn;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.regex.*;

/**

@author Phil Norman
*/

public class HyperlinkHighlighter implements Highlighter {
	/** The underlined blue standard hyperlink style. */
	private final Style style = new Style(Color.blue, null, null, Boolean.TRUE);

	private static class HyperLinker {
		Pattern pattern;
		int relevantGroup;
		String command;
		boolean runInTab;
		
		/**
		 * We need a regular expression to match with, the index of the
		 * group within the expression that should be highlighted (0 if
		 * you want the whole expression highlighted, and the command
		 * to be run when the link is followed.
		 */
		HyperLinker(String regularExpression, int relevantGroup, String command, boolean runInTab) {
			this.pattern = Pattern.compile(regularExpression);
			this.relevantGroup = relevantGroup;
			this.command = command;
			this.runInTab = runInTab;
		}
		
		Matcher matcher(String line) {
			return pattern.matcher(line);
		}
		
		String command(Matcher matcher) {
			return matcher.replaceFirst(command);
		}
		
		boolean runInTab() {
			return runInTab;
		}
	}
	
	private ArrayList linkers = new ArrayList();
	{
		linkers.add(new HyperLinker("(\\b(http|https|ftp):/*[^\\s:\"]+(:\\d+)?[/\\w\\.\\?&=\\+]*)", 1, "open $1", false));
		linkers.add(new HyperLinker("(?:^| |\")(/[^ :\"]+\\.\\w+([\\d:]+)?)", 1, "vi $1", false));
		linkers.add(new HyperLinker("(\\btelnet:([a-zA-Z0-9-_\\.]+))", 1, "telnet $2", true));
		linkers.add(new HyperLinker("(\\brsh:([a-zA-Z0-9-_\\.]+))", 1, "rsh $2", true));
		linkers.add(new HyperLinker("(\\bssh:([a-zA-Z0-9-_\\.@]+))", 1, "ssh $2", true));
	}

	public String getName() {
		return "Hyperlink Highlighter";
	}
	
	/** Request to add highlights to all lines of the view from the index given onwards. */
	public void addHighlights(JTextBuffer view, int firstLineIndex) {
		TextBuffer model = view.getModel();
		for (int i = firstLineIndex; i < model.getLineCount(); i++) {
			String line = model.getLine(i);
			addHighlights(view, i, line);
		}
	}
	
	private void addHighlights(JTextBuffer view, int lineIndex, String text) {
		for (int i = 0; i < linkers.size(); ++i) {
			HyperLinker linker = (HyperLinker) linkers.get(i);
			Matcher matcher = linker.matcher(text);
			while (matcher.find()) {
				Location start = new Location(lineIndex, matcher.start(linker.relevantGroup));
				Location end = new Location(lineIndex, matcher.end(linker.relevantGroup));
				Highlight highlight = new Highlight(HyperlinkHighlighter.this, start, end, style);
				highlight.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				view.addHighlight(highlight);
			}
		}
	}

	/** Request to do something when the user clicks on a Highlight generated by this Highlighter. */
	public void highlightClicked(JTextBuffer view, Highlight highlight, String text, MouseEvent event) {
		for (int i = 0; i < linkers.size(); ++i) {
			HyperLinker linker = (HyperLinker) linkers.get(i);
			Matcher matcher = linker.matcher(text);
			while (matcher.find()) {
				String command = linker.command(matcher);
				if (linker.runInTab()) {
					view.getController().openCommandPane(command, true);
				} else {
					try {
						Runtime.getRuntime().exec(command);
					} catch (Exception ex) {
						Log.warn("Couldn't show '" + text + "' (with '" + linker.command(matcher) + "')", ex);
					}
				}
			}
		}
	}
}
