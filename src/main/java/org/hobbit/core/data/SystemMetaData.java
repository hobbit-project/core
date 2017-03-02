/**
 * This file is part of core.
 *
 * core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with core.  If not, see <http://www.gnu.org/licenses/>.
 */
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
