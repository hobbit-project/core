package org.hobbit.core.data;

import java.util.Set;

public class SystemMetaData {

    public String systemUri;
    public String systemName;
    public String systemDescription;
    public String system_image_name;
    public Set<String> implementedApis;

    @Override
    public int hashCode() {
        return ((systemUri == null) ? 0 : systemUri.hashCode());
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
        if (systemUri == null) {
            if (other.systemUri != null)
                return false;
        } else if (!systemUri.equals(other.systemUri))
            return false;
        return true;
    }

}
