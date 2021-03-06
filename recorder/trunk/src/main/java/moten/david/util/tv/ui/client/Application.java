package moten.david.util.tv.ui.client;

import moten.david.util.tv.ui.client.controller.Controller;

public class Application {

	private static Application application;

	public synchronized static Application getInstance() {
		if (application == null)
			application = new Application();
		return application;
	}

	private Controller controller = new Controller();

	public Controller getController() {
		return controller;
	}

	public void setController(Controller controller) {
		this.controller = controller;
	}

	public Application() {

	}
}
