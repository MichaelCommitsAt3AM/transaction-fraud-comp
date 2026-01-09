package io.github.michaelcommitsat3am.transactionfraudcomp.gui.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.util.Callback;

import java.io.IOException;

/**
 * Factory for creating FXMLLoader instances with dependency injection support.
 */
public class FXMLLoaderFactory {

    /**
     * Loads an FXML file with a custom controller factory for dependency injection.
     */
    public static <T> LoadResult<T> load(String fxmlPath, Callback<Class<?>, Object> controllerFactory)
            throws IOException {
        FXMLLoader loader = new FXMLLoader(FXMLLoaderFactory.class.getResource(fxmlPath));
        loader.setControllerFactory(controllerFactory);

        Parent root = loader.load();
        T controller = loader.getController();

        return new LoadResult<>(root, controller);
    }

    /**
     * Result of FXML loading containing both the root node and controller.
     */
    public static class LoadResult<T> {
        private final Parent root;
        private final T controller;

        public LoadResult(Parent root, T controller) {
            this.root = root;
            this.controller = controller;
        }

        public Parent getRoot() {
            return root;
        }

        public T getController() {
            return controller;
        }
    }
}
