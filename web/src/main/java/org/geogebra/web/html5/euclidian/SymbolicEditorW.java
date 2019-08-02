package org.geogebra.web.html5.euclidian;

import org.geogebra.common.awt.GColor;
import org.geogebra.common.awt.GRectangle;
import org.geogebra.common.euclidian.SymbolicEditor;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.geos.GeoInputBox;
import org.geogebra.common.main.App;
import org.geogebra.common.main.Feature;
import org.geogebra.common.util.FormatConverterImpl;
import org.geogebra.common.util.StringUtil;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.himamis.retex.editor.share.event.MathFieldListener;
import com.himamis.retex.editor.share.model.MathSequence;
import com.himamis.retex.editor.web.MathFieldW;

/**
 * MathField-capable editor for EV, Web implementation.
 *
 * @author Laszlo
 */
public class SymbolicEditorW
		implements SymbolicEditor, MathFieldListener, IsWidget {

	public static final int ROUNDING = 8;
	private static final int BORDER_WIDTH = 2;
	private final Kernel kernel;
	private final boolean directFormulaConversion;
	private FlowPanel main;
	private MathFieldW mathField;
	private int fontSize;
	private static final int PADDING_TOP = 16;
	private static final int PADDING_LEFT = 2;
	private GeoInputBox geoIntputBox;
	private GRectangle bounds;
	private Style style;
	private double top;
	private int mainHeight;

	SymbolicEditorW(App app)  {
		this.kernel = app.getKernel();
		directFormulaConversion = app.has(Feature.MOW_DIRECT_FORMULA_CONVERSION);
		fontSize = app.getSettings().getFontSettings().getAppFontSize() + 2;
		createMathField();
	}

	private void createMathField() {
		main = new FlowPanel();
		Canvas canvas = Canvas.createIfSupported();
		mathField = new MathFieldW(new FormatConverterImpl(kernel), main,
				canvas, this,
				directFormulaConversion,
				null);
		main.addStyleName("evInputEditor");
		main.add(mathField);
		style = main.getElement().getStyle();
	}

	@Override
	public void attach(GeoInputBox geoInputBox, GRectangle bounds) {
		this.geoIntputBox = geoInputBox;
		this.bounds = bounds;
		String text = geoInputBox.getTextForEditor();
		main.removeStyleName("hidden");
		updateBounds(bounds);
		updateColors();
		mathField.setText(text, false);
		mathField.setFontSize(fontSize * geoInputBox.getFontSizeMultiplier());
		mathField.setFocus(true);
	}

	private void updateColors() {
		GColor bgColor = geoIntputBox.getBackgroundColor();
		if (bgColor == null) {
			bgColor = GColor.WHITE;
		}
		String fgColorString = GColor.getColorString(geoIntputBox.getObjectColor());
		String bgColorString = GColor.getColorString(bgColor);
		main.getElement().getStyle().setBackgroundColor(bgColorString);
		mathField.setForegroundColor(fgColorString);
		mathField.setBackgroundColor(bgColorString);
		mathField.setBackgroundColorCss("#"+StringUtil.toHexString(bgColor));
	}

	private void updateBounds(GRectangle bounds) {
		this.bounds = bounds;
		double fieldWidth = bounds.getWidth() - PADDING_LEFT;
		style.setLeft(bounds.getX(), Style.Unit.PX);
		top = bounds.getY();
		style.setTop(top, Style.Unit.PX);
		style.setWidth(fieldWidth, Style.Unit.PX);
		setHeight(bounds.getHeight() - 2 * BORDER_WIDTH);
	}

	private void setHeight(double height)  {
		style.setHeight(height , Style.Unit.PX);
		mainHeight = (int) bounds.getHeight();
	}

	@Override
	public void hide() {
		main.addStyleName("hidden");
	}

	@Override
	public void onEnter() {
		geoIntputBox.updateLinkedGeo(mathField.getText());
	}

	@Override
	public void onKeyTyped() {
		adjustHeightAndPosition();
		scrollToEnd();
	}

	private void adjustHeightAndPosition() {
		int height = mathField.getInputTextArea().getOffsetHeight();
		double diff = mainHeight - main.getOffsetHeight();
		setHeight(height - PADDING_TOP - 2 * BORDER_WIDTH);
		top += (diff/2);
		style.setTop(top, Style.Unit.PX);
		geoIntputBox.update();
		mainHeight = main.getOffsetHeight();
	}

	@Override
	public void onCursorMove() {
		scrollToEnd();
	}

	private void scrollToEnd()  {
		MathFieldW.scrollParent(main, PADDING_LEFT);
	}

	@Override
	public void onUpKeyPressed() {
	 	// nothing to do.
	}

	@Override
	public void onDownKeyPressed() {
		// nothing to do.
	}

	@Override
	public String serialize(MathSequence selectionText) {
		return null;
	}

	@Override
	public void onInsertString() {
		// nothing to do.
	}

	@Override
	public boolean onEscape() {
		resetChanges();
		return true;
	}

	private void resetChanges() {
		attach(geoIntputBox, bounds);
	}

	@Override
	public void onTab(boolean shiftDown) {
		// TODO: implement this.
	}

	@Override
	public Widget asWidget() {
		return main;
	}
}
