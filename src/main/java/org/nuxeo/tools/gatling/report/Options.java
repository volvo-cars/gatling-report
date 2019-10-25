/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benoit Delbosc
 */
package org.nuxeo.tools.gatling.report;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;

import java.util.List;

public class Options {

    @Parameter(required = true, description = "SIMULATION.LOG...")
    public List<String> simulations = Lists.newArrayList();

    @Parameter(names = { "--cluster_name", "-c" }, description = "Elastic search cluster name")
    public String clusterName;

    @Parameter(names = { "--hostname", "-h" }, description = "Elastic search hostname")
    public String hostname;

    @Parameter(names = { "--port", "-p" }, description = "Elastic search port.")
    public Integer port;

    @Parameter(names = { "--help"}, description = "Display this message.")
    public Boolean help;

}
