package org.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

abstract class Contact implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String number = "";
    protected final LocalDateTime timeCreated;
    protected LocalDateTime timeLastEdit;

    protected Contact(String number) {
        this.timeCreated = LocalDateTime.now();
        this.timeLastEdit = this.timeCreated;
        setNumber(number);
    }

    protected void touch() {
        timeLastEdit = LocalDateTime.now();
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

    public String getNumberPrintable() {
        return (number == null || number.isEmpty()) ? "[no number]" : number;
    }

    public LocalDateTime getTimeCreated() {
        return timeCreated;
    }

    public LocalDateTime getTimeLastEdit() {
        return timeLastEdit;
    }

    // ----- Polymorphism methods (Main uses only these) -----
    public abstract String getListName();                  // for list/search results
    public abstract void printInfo();                      // record details
    public abstract String[] getEditableFields();          // fields you can edit
    public abstract void setField(String field, String v); // edit a field
    public abstract String getFieldValue(String field);    // read a field value
    public abstract String getSearchText();                // for searching

    // ----- Phone validation (your regex) -----
    public static boolean isValidNumber(String number) {
        if (number == null || number.isEmpty()) {
            return false;
        }
        String regex1 = "[+]?[a-zA-Z0-9]?([\\s-]?[a-zA-Z0-9]{2,})*";
        String regex2 = "[+]?(\\([a-zA-Z0-9]+\\))([\\s-][a-zA-Z0-9]{2,})*";
        String regex3 = "[+]?[a-zA-Z0-9]{1,}[\\s-]\\([a-zA-Z0-9]{2,}\\)([\\s-][a-zA-Z0-9]{2,})*";
        return number.matches(regex1) || number.matches(regex2) || number.matches(regex3);
    }
}

class PersonContact extends Contact {
    private static final long serialVersionUID = 1L;

    private String name;
    private String surname;
    private String birthDate = "[no data]";
    private String gender = "[no data]";

    public PersonContact(String name, String surname, String birthDate, String gender, String number) {
        super(number);
        setName(name);
        setSurname(surname);
        setBirthDate(birthDate);
        setGender(gender);
    }

    public void setName(String name) {
        this.name = (name == null) ? "" : name;
        touch();
    }

    public void setSurname(String surname) {
        this.surname = (surname == null) ? "" : surname;
        touch();
    }

    public void setBirthDate(String birthDateInput) {
        if (birthDateInput == null || birthDateInput.isEmpty()) {
            System.out.println("Bad birth date!");
            this.birthDate = "[no data]";
            touch();
            return;
        }
        try {
            LocalDate.parse(birthDateInput.trim()); // yyyy-MM-dd
            this.birthDate = birthDateInput.trim();
        } catch (Exception e) {
            System.out.println("Bad birth date!");
            this.birthDate = "[no data]";
        }
        touch();
    }

    public void setGender(String genderInput) {
        if (genderInput == null || genderInput.isEmpty()) {
            System.out.println("Bad gender!");
            this.gender = "[no data]";
            touch();
            return;
        }
        String g = genderInput.trim().toUpperCase(Locale.ROOT);
        if (g.equals("M") || g.equals("F")) {
            this.gender = g;
        } else {
            System.out.println("Bad gender!");
            this.gender = "[no data]";
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
        System.out.println("Birth date: " + birthDate);
        System.out.println("Gender: " + gender);
        System.out.println("Number: " + getNumberPrintable());
        System.out.println("Time created: " + getTimeCreated());
        System.out.println("Time last edit: " + getTimeLastEdit());
    }

    @Override
    public String[] getEditableFields() {
        return new String[]{"name", "surname", "birth", "gender", "number"};
    }

    @Override
    public void setField(String field, String v) {
        switch (field) {
            case "name":
                setName(v);
                break;
            case "surname":
                setSurname(v);
                break;
            case "birth":
                setBirthDate(v);
                break;
            case "gender":
                setGender(v);
                break;
            case "number":
                setNumber(v);
                break;
            default:
                break;
        }
    }

    @Override
    public String getFieldValue(String field) {
        switch (field) {
            case "name":
                return name;
            case "surname":
                return surname;
            case "birth":
                return birthDate;
            case "gender":
                return gender;
            case "number":
                return getNumberPrintable();
            default:
                return "";
        }
    }

    @Override
    public String getSearchText() {
        // append all fields for searching
        return (name + " " + surname + " " + birthDate + " " + gender + " " + getNumberPrintable()).toLowerCase(Locale.ROOT);
    }
}

class OrganizationContact extends Contact {
    private static final long serialVersionUID = 1L;

    private String name;    // organization name
    private String address;

    public OrganizationContact(String name, String address, String number) {
        super(number);
        setName(name);
        setAddress(address);
    }

    public void setName(String name) {
        this.name = (name == null) ? "" : name;
        touch();
    }

    public void setAddress(String address) {
        this.address = (address == null) ? "" : address;
        touch();
    }

    @Override
    public String getListName() {
        return name;
    }

    @Override
    public void printInfo() {
        System.out.println("Organization name: " + name);
        System.out.println("Address: " + address);
        System.out.println("Number: " + getNumberPrintable());
        System.out.println("Time created: " + getTimeCreated());
        System.out.println("Time last edit: " + getTimeLastEdit());
    }

    @Override
    public String[] getEditableFields() {
        return new String[]{"name", "address", "number"};
    }

    @Override
    public void setField(String field, String v) {
        switch (field) {
            case "name":
                setName(v);
                break;
            case "address":
                setAddress(v);
                break;
            case "number":
                setNumber(v);
                break;
            default:
                break;
        }
    }

    @Override
    public String getFieldValue(String field) {
        switch (field) {
            case "name":
                return name;
            case "address":
                return address;
            case "number":
                return getNumberPrintable();
            default:
                return "";
        }
    }

    @Override
    public String getSearchText() {
        return (name + " " + address + " " + getNumberPrintable()).toLowerCase(Locale.ROOT);
    }
}

public class App {
    static Scanner sc = new Scanner(System.in, StandardCharsets.UTF_8.name());
    static List<Contact> contacts = new ArrayList<>();
    static String fileName = null;

    public static void main(String[] args) {
        if (args.length > 0) {
            fileName = args[0];
            load();
        }

        while (true) {
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
                    count();
                    break;
                case "exit":
                    return;
                default:
                    break;
            }

            // empty line between actions
            System.out.println();
        }
    }

    // ---------- Save / Load ----------
    static void save() {
        if (fileName == null) return;

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(contacts);
        } catch (IOException ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    static void load() {
        File f = new File(fileName);
        if (!f.exists()) {
            contacts = new ArrayList<>();
            save(); // create empty file
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
            Object obj = ois.readObject();
            contacts = (List<Contact>) obj;
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            contacts = new ArrayList<>();
        }
    }

    static void printSavedIfFile() {
        if (fileName != null) {
            System.out.println("Saved");
        }
    }

    // ---------- Menu actions ----------
    private static void add() {
        System.out.print("Enter the type (person, organization): ");
        String type = sc.nextLine().trim();

        if ("person".equals(type)) {
            System.out.print("Enter the name: ");
            String name = sc.nextLine();

            System.out.print("Enter the surname: ");
            String surname = sc.nextLine();

            System.out.print("Enter the birth date: ");
            String birth = sc.nextLine();

            System.out.print("Enter the gender (M, F): ");
            String gender = sc.nextLine();

            System.out.print("Enter the number: ");
            String number = sc.nextLine();

            contacts.add(new PersonContact(name, surname, birth, gender, number));
            save();
            System.out.println("The record added.");
            printSavedIfFile();

        } else if ("organization".equals(type)) {
            System.out.print("Enter the organization name: ");
            String orgName = sc.nextLine();

            System.out.print("Enter the address: ");
            String address = sc.nextLine();

            System.out.print("Enter the number: ");
            String number = sc.nextLine();

            contacts.add(new OrganizationContact(orgName, address, number));
            save();
            System.out.println("The record added.");
            printSavedIfFile();
        }
    }

    private static void count() {
        System.out.println("The Phone Book has " + contacts.size() + " records.");
    }

    // ---------- List flow ----------
    private static void listMenu() {
        for (int i = 0; i < contacts.size(); i++) {
            System.out.println((i + 1) + ". " + contacts.get(i).getListName());
        }

        while (true) {
            System.out.print("[list] Enter action ([number], back): ");
            String cmd = sc.nextLine().trim();

            if ("back".equals(cmd)) return;

            if (isNumber(cmd)) {
                int idx = Integer.parseInt(cmd) - 1;
                if (idx >= 0 && idx < contacts.size()) {
                    recordMenu(idx);
                    System.out.println();
                    return;
                }
            }
        }
    }

    // ---------- Search flow ----------
    private static void searchMenu() {
        List<Integer> lastResults = doSearchOnce();

        while (true) {
            System.out.print("[search] Enter action ([number], back, again): ");
            String cmd = sc.nextLine().trim();

            if ("back".equals(cmd)) return;

            if ("again".equals(cmd)) {
                lastResults = doSearchOnce();
                continue;
            }

            if (isNumber(cmd)) {
                int pos = Integer.parseInt(cmd) - 1;
                if (pos >= 0 && pos < lastResults.size()) {
                    int realIndex = lastResults.get(pos);
                    recordMenu(realIndex);
                    System.out.println();
                    return;
                }
            }
        }
    }

    private static List<Integer> doSearchOnce() {
        System.out.print("Enter search query: ");
        String q = sc.nextLine();

        Pattern pattern;
        try {
            pattern = Pattern.compile(q, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            // if user typed bad regex, treat it as plain text
            pattern = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE);
        }

        List<Integer> results = new ArrayList<>();
        for (int i = 0; i < contacts.size(); i++) {
            String text = contacts.get(i).getSearchText();
            if (pattern.matcher(text).find()) {
                results.add(i);
            }
        }

        System.out.println("Found " + results.size() + " results:");
        for (int i = 0; i < results.size(); i++) {
            System.out.println((i + 1) + ". " + contacts.get(results.get(i)).getListName());
        }
        return results;
    }

    // ---------- Record flow ----------
    private static void recordMenu(int idx) {
        Contact c = contacts.get(idx);
        c.printInfo();
        System.out.println();

        while (true) {
            System.out.print("[record] Enter action (edit, delete, menu): ");
            String action = sc.nextLine().trim();

            if ("menu".equals(action)) return;

            if ("delete".equals(action)) {
                contacts.remove(idx);
                save();
                System.out.println("The record removed!");
                printSavedIfFile();
                return;
            }

            if ("edit".equals(action)) {
                editRecord(c);
                save();
                printSavedIfFile();
                c.printInfo();
                System.out.println();
            }
        }
    }

    private static void editRecord(Contact c) {
        String[] fields = c.getEditableFields();
        System.out.print("Select a field (" + String.join(", ", fields) + "): ");
        String field = sc.nextLine().trim();

        System.out.print("Enter " + field + ": ");
        String value = sc.nextLine();

        c.setField(field, value);
        System.out.println("The record updated!");
    }

    // ---------- helpers ----------
    private static boolean isNumber(String s) {
        if (s == null || s.isEmpty()) return false;
        for (char ch : s.toCharArray()) {
            if (!Character.isDigit(ch)) return false;
        }
        return true;
    }
}
