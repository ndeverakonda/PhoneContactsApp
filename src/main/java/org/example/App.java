package org.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

abstract class Record implements Serializable {
    private String number = "";
    private final LocalDateTime timeCreated;
    private LocalDateTime timeLastEdit;

    protected Record() {
        timeCreated = LocalDateTime.now();
        timeLastEdit = timeCreated;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        if (number == null || number.isEmpty()) {
            this.number = "";
            touch();
            return;
        }
        if (isValidNumber(number)) {
            this.number = number;
        } else {
            System.out.println("Wrong number format!");
            this.number = "";
        }
        touch();
    }

    public boolean hasNumber() {
        return number != null && !number.isEmpty();
    }

    public LocalDateTime getTimeCreated() {
        return timeCreated;
    }

    public LocalDateTime getTimeLastEdit() {
        return timeLastEdit;
    }

    protected void touch() {
        timeLastEdit = LocalDateTime.now();
    }

    public abstract List<String> getEditableFields();
    public abstract String getFieldValue(String field);
    public abstract void setFieldValue(String field, String value);
    public abstract String getListName();
    public abstract void printInfo();

    public String getSearchText() {
        StringBuilder sb = new StringBuilder();
        for (String f : getEditableFields()) {
            sb.append(getFieldValue(f)).append(" ");
        }
        sb.append(hasNumber() ? getNumber() : "[no number]").append(" ");
        sb.append(getTimeCreated()).append(" ");
        sb.append(getTimeLastEdit());
        return sb.toString();
    }

    private boolean isValidNumber(String number) {
        String s = number;
        if (s.startsWith("+")) s = s.substring(1);

        List<String> groups = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == ' ' || ch == '-') {
                if (cur.length() > 0) {
                    groups.add(cur.toString());
                    cur.setLength(0);
                }
            } else {
                cur.append(ch);
            }
        }
        if (cur.length() > 0) groups.add(cur.toString());

        if (groups.isEmpty()) return false;

        int parenGroups = 0;
        for (int idx = 0; idx < groups.size(); idx++) {
            String g = groups.get(idx);

            boolean wrapped = g.startsWith("(") && g.endsWith(")");
            boolean hasAnyParen = g.indexOf('(') >= 0 || g.indexOf(')') >= 0;

            if (wrapped) {
                parenGroups++;
                if (idx > 1) return false;
                g = g.substring(1, g.length() - 1);
                if (g.isEmpty()) return false;
            } else {
                if (hasAnyParen) return false;
            }

            for (int k = 0; k < g.length(); k++) {
                char c = g.charAt(k);
                if (!Character.isLetterOrDigit(c)) return false;
            }

            if (idx == 0) {
                if (g.length() < 1) return false;
            } else {
                if (g.length() < 2) return false;
            }
        }
        return parenGroups <= 1;
    }
}

class Person extends Record {
    private static final long serialVersionUID = 1L;
    private String name;
    private String surname;
    private String birth = "";
    private String gender = "";

    public Person(String name, String surname) {
        super();
        this.name = name;
        this.surname = surname;
    }

    @Override
    public List<String> getEditableFields() {
        return Arrays.asList("name", "surname", "birth", "gender", "number");
    }

    @Override
    public String getFieldValue(String field) {
        switch (field) {
            case "name": return name;
            case "surname": return surname;
            case "birth": return birth.isEmpty() ? "[no data]" : birth;
            case "gender": return gender.isEmpty() ? "[no data]" : gender;
            case "number": return hasNumber() ? getNumber() : "[no number]";
            default: return "";
        }
    }

    @Override
    public void setFieldValue(String field, String value) {
        switch (field) {
            case "name":
                name = value;
                touch();
                break;
            case "surname":
                surname = value;
                touch();
                break;
            case "birth":
                setBirth(value);
                break;
            case "gender":
                setGender(value);
                break;
            case "number":
                setNumber(value);
                break;
            default:
                break;
        }
    }

    private void setBirth(String birth) {
        if (birth == null || birth.isEmpty()) {
            System.out.println("Bad birth date!");
            this.birth = "";
            touch();
            return;
        }
        try {
            LocalDate.parse(birth);
            this.birth = birth;
        } catch (Exception e) {
            System.out.println("Bad birth date!");
            this.birth = "";
        }
        touch();
    }

    private void setGender(String gender) {
        if (gender == null || gender.isEmpty()) {
            System.out.println("Bad gender!");
            this.gender = "";
            touch();
            return;
        }
        if (!gender.equals("M") && !gender.equals("F")) {
            System.out.println("Bad gender!");
            this.gender = "";
        } else {
            this.gender = gender;
        }
        touch();
    }

    @Override
    public String getListName() {
        return name + " " + surname;
    }

    @Override
    public void printInfo() {
        System.out.println("Name: " + name);
        System.out.println("Surname: " + surname);
        System.out.println("Birth date: " + (birth.isEmpty() ? "[no data]" : birth));
        System.out.println("Gender: " + (gender.isEmpty() ? "[no data]" : gender));
        System.out.println("Number: " + (hasNumber() ? getNumber() : "[no number]"));
        System.out.println("Time created: " + getTimeCreated());
        System.out.println("Time last edit: " + getTimeLastEdit());
    }
}

class Organization extends Record {
    private static final long serialVersionUID = 1L;
    private String name;
    private String address;

    public Organization(String name, String address) {
        super();
        this.name = name;
        this.address = address;
    }

    @Override
    public List<String> getEditableFields() {
        return Arrays.asList("name", "address", "number");
    }

    @Override
    public String getFieldValue(String field) {
        switch (field) {
            case "name": return name;
            case "address": return address;
            case "number": return hasNumber() ? getNumber() : "[no number]";
            default: return "";
        }
    }

    @Override
    public void setFieldValue(String field, String value) {
        switch (field) {
            case "name":
                name = value;
                touch();
                break;
            case "address":
                address = value;
                touch();
                break;
            case "number":
                setNumber(value);
                break;
            default:
                break;
        }
    }

    @Override
    public String getListName() {
        return name;
    }

    @Override
    public void printInfo() {
        System.out.println("Organization name: " + name);
        System.out.println("Address: " + address);
        System.out.println("Number: " + (hasNumber() ? getNumber() : "[no number]"));
        System.out.println("Time created: " + getTimeCreated());
        System.out.println("Time last edit: " + getTimeLastEdit());
    }
}

class PhoneBook implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<Record> records = new ArrayList<>();

    public int count() {
        return records.size();
    }

    public void add(Record r) {
        records.add(r);
    }

    public void remove(int idx) {
        records.remove(idx);
    }

    public Record get(int idx) {
        return records.get(idx);
    }

    public List<Record> getRecords() {
        return records;
    }
}

public class App {
    static Scanner sc = new Scanner(System.in, StandardCharsets.UTF_8.name());

    static PhoneBook book;
    static String filePath = null;

    static void setScannerInput(String input) {
        sc = new Scanner(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8.name());
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            filePath = args[0];
            System.out.println("open " + filePath);
            book = load(filePath);
        } else {
            book = new PhoneBook();
        }

        boolean keepSearching = true;
        while (keepSearching) {
            System.out.print("[menu] Enter action (add, list, search, count, exit): ");
            String action = sc.nextLine().trim();

            switch (action) {
                case "add":
                    add();
                    break;
                case "list":
                    listMenu();
                    break;
                case "search":
                    searchMenu();
                    break;
                case "count":
                    System.out.println("The Phone Book has " + book.count() + " records.");
                    break;
                case "exit":
                    return;
                default:
                    break;
            }

            System.out.println(); // ✅ ALWAYS one empty line after each action
        }
    }

    static void add() {
        System.out.print("Enter the type (person, organization): ");
        String type = sc.nextLine().trim();

        if ("person".equals(type)) {
            System.out.print("Enter the name: ");
            String name = sc.nextLine();
            System.out.print("Enter the surname: ");
            String surname = sc.nextLine();

            Person p = new Person(name, surname);

            System.out.print("Enter the birth date: ");
            p.setFieldValue("birth", sc.nextLine());

            System.out.print("Enter the gender (M, F): ");
            p.setFieldValue("gender", sc.nextLine().trim());

            System.out.print("Enter the number: ");
            p.setFieldValue("number", sc.nextLine());

            book.add(p);
        } else if ("organization".equals(type)) {
            System.out.print("Enter the organization name: ");
            String name = sc.nextLine();
            System.out.print("Enter the address: ");
            String address = sc.nextLine();

            Organization o = new Organization(name, address);

            System.out.print("Enter the number: ");
            o.setFieldValue("number", sc.nextLine());

            book.add(o);
        }

        System.out.println("The record added.");
        saveIfNeeded();
    }

    static void listMenu() {
        if (book.count() == 0) {
            System.out.println("No records to list!");
            return;
        }
        for (int i = 0; i < book.count(); i++) {
            System.out.println((i + 1) + ". " + book.get(i).getListName());
        }

        System.out.print("[list] Enter action ([number], back): ");
        String cmd = sc.nextLine().trim();
        if ("back".equals(cmd)) return;

        int idx;
        try {
            idx = Integer.parseInt(cmd) - 1;
        } catch (NumberFormatException e) {
            return;
        }
        if (idx < 0 || idx >= book.count()) return;

        System.out.println();
        System.out.println();
        recordMenu(idx);
    }

    static void searchMenu() {
        if (book.count() == 0) {
            System.out.println("No records to search!");
            return;
        }

        boolean keepSearching = true;
        while (keepSearching) {
            System.out.print("Enter search query: ");
            String query = sc.nextLine();

            List<Integer> found = searchIndexes(query);

            System.out.println("Found " + found.size() + " results:");
            for (int i = 0; i < found.size(); i++) {
                System.out.println((i + 1) + ". " + book.get(found.get(i)).getListName());
            }

            System.out.print("[search] Enter action ([number], back, again): ");
            String cmd = sc.nextLine().trim();

            if ("back".equals(cmd)) return;
            if ("again".equals(cmd)) {
                System.out.println();
                continue;
            }

            int pick;
            try {
                pick = Integer.parseInt(cmd) - 1;
            } catch (NumberFormatException e) {
                return;
            }
            if (pick < 0 || pick >= found.size()) return;

            System.out.println();
            System.out.println();
            recordMenu(found.get(pick));
            System.out.println();
            keepSearching = false;
        }
    }

    static List<Integer> searchIndexes(String query) {
        List<Integer> result = new ArrayList<>();
        Pattern p;

        try {
            p = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            p = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE);
        }

        for (int i = 0; i < book.count(); i++) {
            String hay = book.get(i).getSearchText();
            if (p.matcher(hay).find()) {
                result.add(i);
            }
        }
        return result;
    }

    static void recordMenu(int idx) {
        Record r = book.get(idx);
        r.printInfo();

        while (true) {
            System.out.print("[record] Enter action (edit, delete, menu): ");
            String cmd = sc.nextLine().trim();

            if ("menu".equals(cmd)) {
                System.out.println();
                return;
            }

            if ("delete".equals(cmd)) {
                book.remove(idx);
                System.out.println("The record removed!");
                saveIfNeeded();
                System.out.println(); // ✅ blank line after action
                return;
            }

            if ("edit".equals(cmd)) {
                List<String> fields = r.getEditableFields();

                System.out.print("Select a field (");
                for (int i = 0; i < fields.size(); i++) {
                    System.out.print(fields.get(i));
                    if (i + 1 < fields.size()) System.out.print(", ");
                }
                System.out.println("): ");

                String field = sc.nextLine().trim();
                System.out.print("Enter " + field + ": ");
                String value = sc.nextLine();

                r.setFieldValue(field, value);
                System.out.println("Saved");
                saveIfNeeded();
                r.printInfo();
                System.out.println(); // ✅ blank line after action
            }
        }
    }

    static void saveIfNeeded() {
        if (filePath != null) {
            save(filePath, book);
        }
    }

    static void save(String path, PhoneBook pb) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(pb);
        } catch (IOException e) {
            System.err.println("Failed to save phone book: " + e.getMessage());
        }
    }

    static PhoneBook load(String path) {
        File f = new File(path);
        if (!f.exists()) return new PhoneBook();

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            Object obj = ois.readObject();
            if (obj instanceof PhoneBook) {
                return (PhoneBook) obj;
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Failed to load phone book: " + e.getMessage());
        }
        return new PhoneBook();
    }
}
