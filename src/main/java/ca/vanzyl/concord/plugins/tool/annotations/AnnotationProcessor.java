package ca.vanzyl.concord.plugins.tool.annotations;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

/*

helm <-- commandName
  commands
    - init
    - repo
    - install <chart>
    - upgrade <chart>

*/
public class AnnotationProcessor
{
    /**
     * Construct a CLI based on command name, and a command instance that has been configured. The command instance
     * is configured from the parameters passed to Concord.
     *
     * @param commandName The command name like 'packer' or 'helm'
     * @param command The javax.inject annotated command instance that has been configured
     * @return
     * @throws Exception
     */
    public List<String> process(String commandName, Object command) throws Exception {

        List<String> arguments = Lists.newArrayList(commandName);

        for (Field field : command.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            Object operand = field.get(command);
            if (operand != null) {
                if (primitive(operand)) {
                    processAnnotations(field, command, command, arguments);
                } else {
                    processAnnotation(field.getAnnotation(Omit.class), operand, command, field, arguments);
                    for (Field configuration : operand.getClass().getDeclaredFields()) {
                        processAnnotations(configuration, operand, command, arguments);
                    }
                }
            }
        }
        return arguments;
    }

    // Are we operating on primitives of collections of primitives
    boolean primitive(Object operand) {
        TypeToken<List<String>> stringList = new TypeToken<List<String>>() {};
        return operand.getClass().isPrimitive() ||
                Boolean.class.isAssignableFrom(operand.getClass())
                || String.class.isAssignableFrom(operand.getClass())
                || stringList.getRawType().isAssignableFrom(operand.getClass());
    }

    private void processAnnotations(Field field, Object operand, Object command, List<String> arguments) throws Exception
    {
        if(field.getAnnotations().length == 2) {
            processAnnotation(field.getAnnotation(Option.class), operand, command, field, arguments);
            processAnnotation(field.getAnnotation(OptionAsCsv.class), operand, command, field, arguments);
            processAnnotation(field.getAnnotation(OptionWithEquals.class), operand, command, field, arguments);
            processAnnotation(field.getAnnotation(KeyValue.class), operand, command, field, arguments);
            processAnnotation(field.getAnnotation(Flag.class), operand, command, field, arguments);
        } else {
            processField(operand, command, field, arguments);
        }
    }

    private void processAnnotation(Option option, Object operand, Object command, Field field, List<String> arguments) throws Exception {
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

    private void processAnnotation(OptionAsCsv option, Object operand, Object command, Field field, List<String> arguments) throws Exception {
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
    private void processAnnotation(OptionWithEquals optionWithEquals, Object operand, Object command, Field field, List<String> arguments) throws Exception {
        if (optionWithEquals != null) {
            Object value = field.get(operand);
            if (value != null) {
                arguments.add(optionWithEquals.name()[0] + "=" + value);
            }
        }
    }

    private void processAnnotation(Flag flag, Object operand, Object command, Field field, List<String> arguments) throws Exception {
        if (flag != null) {
            arguments.add(flag.name()[0]);
        }
    }

    // helm --set "foo=bar"
    private void processAnnotation(KeyValue keyValue, Object operand, Object command, Field field, List<String> arguments) throws Exception {
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

    // In the case of Helm the install/upgrade commands imply operating on a chart but 'chart' doesn't show up in the CLI
    // invocation at all:
    //
    // helm install <omit chart> --name xxx --values yyy
    //
    private void processAnnotation(Omit omit, Object operand, Object command, Field field, List<String> arguments) throws Exception {
        if (omit == null) {
            arguments.add(field.getName());
        }
    }

    private void processField(Object operand, Object command, Field field, List<String> arguments) throws Exception {
        field.setAccessible(true);
        Object value = field.get(operand);
        if(value != null) {
            arguments.add((String) value);
        }
    }
}
