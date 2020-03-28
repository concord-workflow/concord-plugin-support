package ca.vanzyl.concord.plugins.tool.annotations;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public class AnnotationProcessor
{
    public static List<String> cliArgumentsFromAnnotations(String commandName, Object command) throws Exception {
        //
        // eksctl create cluster --config-file cluster.yaml --kubeconfig /home/concord/.kube/config
        //
        // kubectl apply -f 00-helm/tiller-rbac.yml
        //
        List<String> arguments = Lists.newArrayList();

        // Running inside Guice vs not. We get the generated proxy when running in Guice and have to reach up to the superclass
        Value v = command.getClass().getSuperclass().getAnnotation(Value.class);
        if (v == null) {
            v = command.getClass().getAnnotation(Value.class);
        }
        if (v != null) {
            arguments.add(v.value());
        } else {
            arguments.add(commandName);
        }

        for (Field field : command.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            Object operand = field.get(command);
            if (operand != null) {
                if (primitive(operand)) {
                    if(field.getAnnotations().length == 2) {
                        processAnnotation(field.getAnnotation(Option.class), command, command, field, arguments);
                        processAnnotation(field.getAnnotation(OptionAsCsv.class), command, command, field, arguments);
                        processAnnotation(field.getAnnotation(OptionWithEquals.class), command, field, arguments);
                        processAnnotation(field.getAnnotation(Flag.class), command, field, arguments);
                    } else {
                        processField(command, field, arguments);
                    }
                } else {
                    processAnnotation(field.getAnnotation(Omit.class), operand, field, arguments);
                    for (Field configuration : operand.getClass().getDeclaredFields()) {
                        if(configuration.getAnnotations().length == 2) {
                            processAnnotation(configuration.getAnnotation(Option.class), operand, command, configuration, arguments);
                            processAnnotation(configuration.getAnnotation(OptionAsCsv.class), command, command, field, arguments);
                            processAnnotation(configuration.getAnnotation(OptionWithEquals.class), command, field, arguments);
                            processAnnotation(configuration.getAnnotation(KeyValue.class), operand, configuration, arguments);
                            processAnnotation(configuration.getAnnotation(Flag.class), operand, configuration, arguments);
                        } else {
                            processField(operand, configuration, arguments);
                        }
                    }
                }
            }
        }
        return arguments;
    }

    static boolean primitive(Object operand) {
        TypeToken<List<String>> stringList = new TypeToken<List<String>>() {};
        return operand.getClass().isPrimitive() ||
                Boolean.class.isAssignableFrom(operand.getClass())
                || String.class.isAssignableFrom(operand.getClass())
                || stringList.getRawType().isAssignableFrom(operand.getClass());
    }

    static void processAnnotation(Option option, Object operand, Object command, Field field, List<String> arguments) throws Exception {
        if (option != null) {
            field.setAccessible(true);
            Object value = field.get(operand);
            if (value != null) {
                // This is specifically for Helm install vs upgrade where install uses the --name parameter and its omitted in upgrade
                if (!option.omitFor().equals(command.getClass())) {
                    arguments.add(option.name()[0]);
                }
                arguments.add((String) value);
            }
        }
    }

    static void processAnnotation(OptionAsCsv option, Object operand, Object command, Field field, List<String> arguments) throws Exception {
        if (option != null) {
            field.setAccessible(true);
            Object value = field.get(operand);
            if (value != null) {
                arguments.add(option.name()[0] + "=" + String.join(",", ((List<String>)value)));
            }
        }
    }

    // kubectl: kubectl --validate=false
    // packer: packer build -parallel-builds=1
    static void processAnnotation(OptionWithEquals optionWithEquals, Object operand, Field field, List<String> arguments) throws Exception {
        if (optionWithEquals != null) {
            Object value = field.get(operand);
            if (value != null) {
                arguments.add(optionWithEquals.name()[0] + "=" + value);
            }
        }
    }

    static void processAnnotation(Flag flag, Object operand, Field field, List<String> arguments) throws Exception {
        if (flag != null) {
            arguments.add(flag.name()[0]);
        }
    }

    // helm --set "foo=bar"
    static void processAnnotation(KeyValue keyValue, Object operand, Field field, List<String> arguments) throws Exception {
        if(keyValue != null) {
            field.setAccessible(true);
            Object fieldValue = field.get(operand);
            if (fieldValue != null) {
                // --set
                String parameter = keyValue.name();
                List<String> kvs = (List<String>) fieldValue;
                for (String e : kvs) {
                    // --set "ingress.hostname=bob.fetesting.com"
                    arguments.add(parameter);
                    arguments.add(e);
                }
            }
        }
    }

    // helm install <omit chart> --name xxx --values yyy
    static void processAnnotation(Omit omit, Object operand, Field field, List<String> arguments) throws Exception {
        if (omit == null) {
            arguments.add(field.getName());
        }
    }

    static void processField(Object operand, Field field, List<String> arguments) throws Exception {
        field.setAccessible(true);
        Object value = field.get(operand);
        if(value != null) {
            arguments.add((String) value);
        }
    }
}
