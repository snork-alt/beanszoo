package com.dataheaps.beanszoo.lifecycle;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by admin on 9/2/17.
 */
public class YamlConfigurationReader implements ConfigurationReader {

    public static class YamlConstructor extends Constructor {

        Properties props = new Properties();

        class EnvConstruct extends AbstractConstruct {

            public Object construct(Node node) {
                String varName = (String) constructScalar((ScalarNode) node);
                String ret = props.getProperty(varName);
                if (ret == null)
                    ret = System.getenv().get(varName);
                if (ret == null)
                    throw new IllegalArgumentException("Environment variable " + varName + " is not defined");
                return ret;
            }
        }

        class ClassConstruct extends AbstractConstruct {

            public Object construct(Node node) {
                try {
                    String varName = (String) constructScalar((ScalarNode) node);
                    return Class.forName(varName);
                }
                catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        class InstanceConstruct extends AbstractConstruct {

            public Object construct(Node node) {

                String className = null;
                Map<String, Object> config = new HashMap<>();

                if (node instanceof ScalarNode) {
                    className = (String) constructScalar((ScalarNode) node);
                }
                else if (node instanceof MappingNode) {
                    Map<Object, Object> values = constructMapping((MappingNode) node);
                    className = (String) values.get("of");
                    if (values.containsKey("with"))
                        config = (Map<String,Object>) values.get("with");
                }
                else {
                    throw new RuntimeException("Invalid node type for !instance");
                }

                try {

                    return new InstanceConfiguration(Class.forName(className), config);
                }
                catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

            }
        }

        public YamlConstructor(Properties props) {
            this.props = props;
            this.yamlConstructors.put(new Tag("!property"), new EnvConstruct());
            this.yamlConstructors.put(new Tag("!instance"), new InstanceConstruct());
            this.yamlConstructors.put(new Tag("!class"), new ClassConstruct());
        }
    }


    Properties props = new Properties();

    @Override
    public Configuration load(String path) throws Exception {

        FileSystemManager fs = VFS.getManager();
        FileObject yamlFile = fs.resolveFile(path);

        FileObject propsFile = fs.resolveFile(FilenameUtils.removeExtension(path) + ".properties");
        if (propsFile.exists()) {
            InputStream is = propsFile.getContent().getInputStream();
            props.load(is);
            is.close();
        }

        YamlConstructor ctor = new YamlConstructor(props);
        Yaml loader = new Yaml(ctor);
        loader.setBeanAccess(BeanAccess.PROPERTY);

        InputStream yamlStream = yamlFile.getContent().getInputStream();
        Configuration config = loader.loadAs(yamlStream, Configuration.class);
        yamlStream.close();

        return config;

    }
}
