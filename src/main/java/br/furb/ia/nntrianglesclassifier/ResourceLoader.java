package br.furb.ia.nntrianglesclassifier;

import java.io.File;
import java.net.URL;

/**
 * Created by Thomas.Adriano on 09/06/2016.
 */
public class ResourceLoader {

    public static File[] getResources(String path) {
        return getResource(path).listFiles();
    }

    public static File getResource(String path) {
        URL url = ResourceLoader.class.getResource(path);
        if (url == null || url.getFile() == null) {
            throw new RuntimeException("Recurso "+path+" não encontrado.");
        }
        return new File(url.getFile());
    }

}
