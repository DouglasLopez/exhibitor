/*
 *
 *  Copyright 2011 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.netflix.exhibitor.core.processes;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.config.EncodedConfigParser;
import com.netflix.exhibitor.core.config.InstanceConfig;
import com.netflix.exhibitor.core.config.StringConfigs;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Pattern;

class Details
{
    final File zooKeeperDirectory;
    final File dataDirectory;
    final File configDirectory;
    final String logPaths;
    final String zooKeeperJarPath;
    final Properties properties;

    Details(Exhibitor exhibitor) throws IOException
    {
        InstanceConfig config = exhibitor.getConfigManager().getConfig();

        this.zooKeeperDirectory = new File(config.getString(StringConfigs.ZOOKEEPER_INSTALL_DIRECTORY));
        this.dataDirectory = new File(config.getString(StringConfigs.ZOOKEEPER_DATA_DIRECTORY));

        configDirectory = new File(zooKeeperDirectory, "conf");
        logPaths = findJar(new File(zooKeeperDirectory, "lib"), "(.*log4j.*)|(.*slf4j.*)");
        zooKeeperJarPath = findJar(this.zooKeeperDirectory, "zookeeper.*");

        properties = new Properties();
        if ( isValid() )
        {
            EncodedConfigParser     parser = new EncodedConfigParser(exhibitor.getConfigManager().getConfig().getString(StringConfigs.ZOO_CFG_EXTRA));
            properties.putAll(parser.getValues());
            properties.put("dataDir", dataDirectory.getPath());
        }
    }

    boolean isValid()
    {
        return isValidPath(zooKeeperDirectory)
            && isValidPath(dataDirectory)
            && isValidPath(configDirectory)
            ;
    }

    private boolean isValidPath(File directory)
    {
        return directory.getPath().length() > 0;
    }

    private String findJar(File dir, String regex) throws IOException
    {
        if ( !isValid() )
        {
            return "";
        }

        final Pattern pattern = Pattern.compile(regex);
        File[]          files = dir.listFiles
            (
                new FileFilter()
                {
                    @Override
                    public boolean accept(File f)
                    {
                        return pattern.matcher(f.getName()).matches() && f.getName().endsWith(".jar");
                    }
                }
            );

        if ( (files == null) || (files.length == 0) )
        {
            throw new IOException("Could not find " + regex + " jar");
        }

        Iterable<String> transformed = Iterables.transform
            (
                Arrays.asList(files),
                new Function<File, String>()
                {
                    @Override
                    public String apply(File f)
                    {
                        return f.getPath();
                    }
                }
            );
        return Joiner.on(':').join(transformed);
    }
}
