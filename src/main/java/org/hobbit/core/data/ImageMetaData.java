package org.hobbit.core.data;

import java.util.Date;
import java.util.Set;

import org.apache.jena.rdf.model.Model;

public class ImageMetaData {

    public String uri;
    public String name;
    public String description;
    public String mainImage;
    public Set<String> usedImages;
    public Model rdfModel;
    public String source;
    public Date date;
    public String defError;

    @Override
    public int hashCode() {
        return ((uri == null) ? 0 : uri.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SystemMetaData other = (SystemMetaData) obj;
        if (uri == null) {
            if (other.uri != null)
                return false;
        } else if (!uri.equals(other.uri))
            return false;
        return true;
    }
}
