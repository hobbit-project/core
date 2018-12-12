package org.hobbit.core.data;

/**
 * @author Pavel Smirnov. (psmirnov@agtinternational.com / smirnp@gmail.com)
 */
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

public class ExecuteCommandData {

    public String containerId;
    public String[] command;

    public ExecuteCommandData(String containerId, String[] command) {
        this.containerId = containerId;
        this.command = command;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String value) {  this.containerId = value; }

    public String[] getCommand() {
        return command;
    }

    public void setCommand(String[] value) {  this.command = value; }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ExecuteCommandData [containerId=");
        builder.append(containerId);
        builder.append(", command=");
        builder.append(command);
        builder.append("]");
        return builder.toString();
    }
}
