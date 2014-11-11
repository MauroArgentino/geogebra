package geogebra.web.main;

import geogebra.html5.euclidian.EuclidianViewW;
import geogebra.html5.gui.GuiManagerInterfaceW;
import geogebra.html5.main.AppW;
import geogebra.web.gui.GuiManagerW;
import geogebra.web.gui.app.GeoGebraAppFrame;
import geogebra.web.gui.dialog.image.ImageInputDialog;
import geogebra.web.gui.dialog.image.UploadImageDialog;

import com.google.gwt.user.client.Window;

public class BrowserDevice implements GDevice {


	

	@Override
	public FileManager getFileManager(AppW app) {
		return new FileManagerW(app);
	}

	@Override
    public void copyEVtoClipboard(EuclidianViewW ev) {
		Window.open(ev.getExportImageDataUrl(3, true),"_blank", null);
	    
    }

	@Override
    public GuiManagerInterfaceW newGuiManager(AppW appWapplication) {
	    return new GuiManagerW(appWapplication, this);
    }

	@Override
    public void setMinWidth(GeoGebraAppFrame frame) {
		if (Window.getClientWidth() > 760) {
			frame.removeStyleName("minWidth");
			frame.syncPanelSizes();
		} else {
			frame.addStyleName("minWidth");
		}
    }

	@Override
    public boolean isOffline(AppW app) {
	    return !app.getNetworkOperation().isOnline();
    }
	
	public UploadImageDialog getImageInputDialog(AppW app) {
		
		return new ImageInputDialog(app);
	}

}
