import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Obfuscator {

    // Список ключевых слов Java, которые нельзя заменять
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "true", "false", "null", "var", "record", "yield", "sealed", "permits"
    ));

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Введите путь к .java файлу или директории с .java файлами:");
        String path = scanner.nextLine().trim();

        // Убираем кавычки, если пользователь скопировал путь из проводника Windows
        if (path.startsWith("\"") && path.endsWith("\"")) {
            path = path.substring(1, path.length() - 1);
        }

        File target = new File(path);
        File sourceFile;
        File dir;

        // Логика определения: файл это или директория
        if (target.isFile()) {
            if (!target.getName().toLowerCase().endsWith(".java")) {
                System.err.println("Ошибка: указанный файл не является .java файлом.");
                return;
            }
            sourceFile = target;
            dir = target.getParentFile();
            if (dir == null) {
                dir = new File("."); // Запасной вариант, если файл в корне диска
            }
        } else if (target.isDirectory()) {
            dir = target;
            File[] javaFiles = dir.listFiles((d, name) -> name.endsWith(".java"));
            if (javaFiles == null || javaFiles.length == 0) {
                System.err.println("В директории не найдено .java файлов.");
                return;
            }
            sourceFile = javaFiles[0];
            System.out.println("Внимание: выбрана директория. Автоматически обработан первый файл: " + sourceFile.getName());
        } else {
            System.err.println("Ошибка: указанный путь не существует или не является файлом/директорией.");
            return;
        }

        System.out.println("Обработка файла: " + sourceFile.getName());

        try {
            // Читаем исходный код
            String originalCode = new String(Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);

            // Шаг 1 и 2: Удаление комментариев и сжатие
            String cleanedCode = removeCommentsAndCompress(originalCode);

            // Шаг 3: Сбор идентификаторов и генерация новых имен
            Map<String, String> replacements = generateIdentifiers(cleanedCode);

            // Шаг 4: Замена идентификаторов
            String obfuscatedCode = replaceIdentifiers(cleanedCode, replacements);

            // Шаг 5: Получаем новое имя класса (чтобы файл назывался правильно)
            String newClassName = getObfuscatedClassName(originalCode, replacements);

            // Имя нового файла
            File newFile = new File(dir, newClassName + ".java");

            // Записываем результат
            Files.write(newFile.toPath(), obfuscatedCode.getBytes(StandardCharsets.UTF_8));
            System.out.println("Готово! Создан файл: " + newFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Произошла ошибка при работе с файлами: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Произошла непредвиденная ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Находит настоящее имя класса в коде и возвращает его обфусцированный вариант.
     */
    private static String getObfuscatedClassName(String code, Map<String, String> replacements) {
        // Ищем объявление класса: необязательный модификатор (public и т.д.), затем class, затем имя
        Pattern pattern = Pattern.compile("\\b(?:public\\s+)?class\\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\\b");
        Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            String realClassName = matcher.group(1);
            return replacements.getOrDefault(realClassName, "a");
        }
        return "a"; // Запасной вариант, если класс не найден
    }

    /**
     * Удаляет комментарии и лишние пробелы, сохраняя строковые литералы.
     */
    private static String removeCommentsAndCompress(String code) {
        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        boolean inChar = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean lastWasSpace = false;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);

            if (inLineComment) {
                if (c == '\n') inLineComment = false;
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && i + 1 < code.length() && code.charAt(i + 1) == '/') {
                    inBlockComment = false;
                    i++; // Пропускаем '/'
                }
                continue;
            }

            if (inString) {
                sb.append(c);
                if (c == '\\' && i + 1 < code.length()) {
                    sb.append(code.charAt(++i)); // Экранированный символ
                } else if (c == '"') {
                    inString = false;
                }
                lastWasSpace = false;
                continue;
            }

            if (inChar) {
                sb.append(c);
                if (c == '\\' && i + 1 < code.length()) {
                    sb.append(code.charAt(++i));
                } else if (c == '\'') {
                    inChar = false;
                }
                lastWasSpace = false;
                continue;
            }

            // Проверка на начало комментария или строки
            if (c == '/' && i + 1 < code.length()) {
                if (code.charAt(i + 1) == '/') {
                    inLineComment = true;
                    i++;
                    continue;
                } else if (code.charAt(i + 1) == '*') {
                    inBlockComment = true;
                    i++;
                    continue;
                }
            }

            if (c == '"') {
                inString = true;
                sb.append(c);
                lastWasSpace = false;
                continue;
            }

            if (c == '\'') {
                inChar = true;
                sb.append(c);
                lastWasSpace = false;
                continue;
            }

            // Сжатие пробелов
            if (Character.isWhitespace(c)) {
                if (!lastWasSpace) {
                    sb.append(' ');
                    lastWasSpace = true;
                }
            } else {
                sb.append(c);
                lastWasSpace = false;
            }
        }
        return sb.toString().trim();
    }

    /**
     * Находит все пользовательские идентификаторы и генерирует для них новые имена.
     */
    private static Map<String, String> generateIdentifiers(String code) {
        Pattern pattern = Pattern.compile("\\b[a-zA-Z_$][a-zA-Z0-9_$]*\\b");
        Matcher matcher = pattern.matcher(code);

        // LinkedHashSet сохраняет порядок добавления, что делает обфускацию предсказуемой
        Set<String> uniqueIds = new LinkedHashSet<>();
        while (matcher.find()) {
            String word = matcher.group();
            if (!KEYWORDS.contains(word)) {
                uniqueIds.add(word);
            }
        }

        Map<String, String> map = new HashMap<>();
        int count = 0;
        for (String id : uniqueIds) {
            map.put(id, generateName(count++));
        }
        return map;
    }

    /**
     * Генерирует односимвольные или двухсимвольные имена (a, b, ... z, aa, ab...)
     */
    private static String generateName(int index) {
        StringBuilder sb = new StringBuilder();
        if (index < 26) {
            sb.append((char) ('a' + index));
        } else {
            sb.append((char) ('a' + (index / 26) - 1));
            sb.append((char) ('a' + (index % 26)));
        }
        return sb.toString();
    }

    /**
     * Заменяет идентификаторы, не задевая строковые литералы.
     */
    private static String replaceIdentifiers(String code, Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        boolean inChar = false;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);

            if (inString) {
                sb.append(c);
                if (c == '\\' && i + 1 < code.length()) {
                    sb.append(code.charAt(++i));
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }

            if (inChar) {
                sb.append(c);
                if (c == '\\' && i + 1 < code.length()) {
                    sb.append(code.charAt(++i));
                } else if (c == '\'') {
                    inChar = false;
                }
                continue;
            }

            if (c == '"') {
                inString = true;
                sb.append(c);
            } else if (c == '\'') {
                inChar = true;
                sb.append(c);
            } else if (Character.isLetter(c) || c == '_' || c == '$') {
                // Начало идентификатора
                int start = i;
                while (i < code.length() && (Character.isLetterOrDigit(code.charAt(i)) || code.charAt(i) == '_' || code.charAt(i) == '$')) {
                    i++;
                }
                String word = code.substring(start, i);
                sb.append(map.getOrDefault(word, word));
                i--; // Шаг назад, так как цикл for сделает i++
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}