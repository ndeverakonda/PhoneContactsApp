package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private void captureStdout() {
        System.setOut(new PrintStream(out));
    }

    private void captureStderr() {
        System.setErr(new PrintStream(err));
    }

    private String stdout() {
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private String stderr() {
        return new String(err.toByteArray(), StandardCharsets.UTF_8);
    }

    private void setAppInput(String... lines) {
        String joined = String.join("\n", lines) + "\n";
        App.setScannerInput(joined);
    }

    @AfterEach
    void restoreGlobals() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        out.reset();
        err.reset();
        App.book = null;
        App.filePath = null;
        App.sc = new Scanner(System.in, StandardCharsets.UTF_8.name());
    }

    @Test
    void phoneBook_addRemoveCountGet() {
        PhoneBook pb = new PhoneBook();
        assertEquals(0, pb.count());

        Person p = new Person("John", "Smith");
        Organization o = new Organization("Acme", "NYC");

        pb.add(p);
        pb.add(o);

        assertEquals(2, pb.count());
        assertSame(p, pb.get(0));
        assertSame(o, pb.get(1));
        assertEquals(2, pb.getRecords().size());

        pb.remove(0);
        assertEquals(1, pb.count());
        assertSame(o, pb.get(0));
    }

    @Test
    void setNumber_acceptsAndRejectsFormats() {
        captureStdout();
        Person p = new Person("A", "B");

        p.setFieldValue("number", "+1 (234) 567-890");
        assertTrue(p.hasNumber());

        p.setFieldValue("number", "a1b2");
        assertEquals("a1b2", p.getNumber());

        p.setFieldValue("number", "12(3");
        assertFalse(p.hasNumber());
        assertTrue(stdout().contains("Wrong number format!"));

        p.setNumber(null);
        assertFalse(p.hasNumber());
    }

    @Test
    void person_fields_validation_and_defaults() {
        captureStdout();
        Person p = new Person("John", "Smith");

        assertEquals("", p.getFieldValue("unknown"));
        assertEquals("[no data]", p.getFieldValue("birth"));
        assertEquals("[no data]", p.getFieldValue("gender"));
        assertEquals("[no number]", p.getFieldValue("number"));

        p.setFieldValue("birth", "2000-02-29");
        p.setFieldValue("gender", "F");
        assertEquals("2000-02-29", p.getFieldValue("birth"));
        assertEquals("F", p.getFieldValue("gender"));

        p.setFieldValue("birth", "bad-date");
        p.setFieldValue("gender", "X");
        p.setFieldValue("birth", "");
        p.setFieldValue("gender", "");
        p.setFieldValue("unknown", "value");

        assertEquals("[no data]", p.getFieldValue("birth"));
        assertEquals("[no data]", p.getFieldValue("gender"));
        assertTrue(stdout().contains("Bad birth date!"));
        assertTrue(stdout().contains("Bad gender!"));
    }

    @Test
    void organization_fields_and_printInfo() {
        captureStdout();
        Organization o = new Organization("OpenAI", "SF");

        assertEquals("", o.getFieldValue("unknown"));
        assertEquals("OpenAI", o.getListName());

        o.setFieldValue("number", "12(3");
        assertEquals("[no number]", o.getFieldValue("number"));

        o.setFieldValue("name", "OpenAI Labs");
        o.setFieldValue("address", "San Francisco");
        o.setFieldValue("number", "123-45-67");
        o.setFieldValue("unknown", "ignored");

        o.printInfo();
        assertTrue(stdout().contains("Organization name: OpenAI Labs"));
        assertTrue(stdout().contains("Address: San Francisco"));
        assertEquals("123-45-67", o.getNumber());
    }

    @Test
    void searchIndexes_regexAndFallbackLiteral() {
        PhoneBook pb = new PhoneBook();
        Person p1 = new Person("John", "Smith");
        Person p2 = new Person("Alice", "Johnson");
        Organization o = new Organization("OpenAI", "SF");

        pb.add(p1);
        pb.add(p2);
        pb.add(o);
        App.book = pb;

        assertEquals(Arrays.asList(0, 1), App.searchIndexes("john"));
        assertEquals(Collections.singletonList(2), App.searchIndexes("open.*"));
        List<Integer> fallbackMatches = App.searchIndexes("[");
        assertFalse(fallbackMatches.isEmpty());
        assertTrue(fallbackMatches.contains(0));
    }

    @Test
    void add_person_organization_and_unknownType() {
        captureStdout();
        App.book = new PhoneBook();

        setAppInput("person", "John", "Smith", "2001-01-01", "M", "+1 234 567");
        App.add();
        assertEquals(1, App.book.count());
        assertTrue(App.book.get(0) instanceof Person);

        setAppInput("organization", "Acme", "NYC", "111-22-33");
        App.add();
        assertEquals(2, App.book.count());
        assertTrue(App.book.get(1) instanceof Organization);

        setAppInput("other");
        App.add();
        assertEquals(2, App.book.count());
        assertTrue(stdout().contains("The record added."));
    }

    @Test
    void listMenu_empty_invalid_back_and_recordPath() {
        captureStdout();
        App.book = new PhoneBook();

        App.listMenu();
        assertTrue(stdout().contains("No records to list!"));

        App.book.add(new Person("John", "Smith"));

        setAppInput("abc");
        App.listMenu();

        setAppInput("99");
        App.listMenu();

        setAppInput("back");
        App.listMenu();

        setAppInput("1", "menu");
        App.listMenu();
        assertTrue(stdout().contains("Name: John"));
    }

    @Test
    void searchMenu_empty_again_back_invalid_and_pickRecord() {
        captureStdout();
        App.book = new PhoneBook();

        App.searchMenu();
        assertTrue(stdout().contains("No records to search!"));

        App.book.add(new Person("John", "Smith"));
        App.book.add(new Person("Alice", "Johnson"));

        setAppInput("john", "again", "john", "back");
        App.searchMenu();

        setAppInput("john", "NaN");
        App.searchMenu();

        setAppInput("john", "99");
        App.searchMenu();

        setAppInput("john", "1", "menu");
        App.searchMenu();
        assertTrue(stdout().contains("Found 2 results:"));
    }

    @Test
    void recordMenu_menu_delete_edit_and_unknownAction() {
        captureStdout();
        App.book = new PhoneBook();

        Person p = new Person("Jane", "Doe");
        App.book.add(p);

        setAppInput("menu");
        App.recordMenu(0);

        setAppInput("noop", "menu");
        App.recordMenu(0);

        setAppInput("edit", "name", "Janet", "menu");
        App.recordMenu(0);
        assertEquals("Janet", ((Person) App.book.get(0)).getFieldValue("name"));

        setAppInput("delete");
        App.recordMenu(0);
        assertEquals(0, App.book.count());
        assertTrue(stdout().contains("The record removed!"));
        assertTrue(stdout().contains("Saved"));
    }

    @Test
    void saveIfNeeded_savesOnlyWhenPathPresent(@TempDir Path tempDir) {
        App.book = new PhoneBook();
        App.book.add(new Person("A", "B"));

        Path file = tempDir.resolve("book.db");
        App.filePath = null;
        App.saveIfNeeded();
        assertFalse(Files.exists(file));

        App.filePath = file.toString();
        App.saveIfNeeded();
        assertTrue(Files.exists(file));
    }

    @Test
    void saveAndLoad_roundTrip_nonexistent_wrongType_and_ioError(@TempDir Path tempDir) throws IOException {
        captureStderr();

        Path file = tempDir.resolve("phonebook.db");

        PhoneBook pb = new PhoneBook();
        Person p = new Person("John", "Smith");
        p.setFieldValue("birth", "1999-01-01");
        p.setFieldValue("gender", "M");
        p.setFieldValue("number", "+1 234 567 890");
        pb.add(p);

        App.save(file.toString(), pb);
        PhoneBook loaded = App.load(file.toString());
        assertEquals(1, loaded.count());
        assertEquals("John Smith", loaded.get(0).getListName());

        Path missing = tempDir.resolve("missing.bin");
        PhoneBook missingLoad = App.load(missing.toString());
        assertEquals(0, missingLoad.count());

        Path wrongType = tempDir.resolve("wrongtype.bin");
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(wrongType))) {
            oos.writeObject("not-a-phonebook");
        }
        PhoneBook wrongTypeLoad = App.load(wrongType.toString());
        assertEquals(0, wrongTypeLoad.count());

        Path badSaveTarget = tempDir.resolve("dir-target");
        Files.createDirectory(badSaveTarget);
        App.save(badSaveTarget.toString(), pb);
        assertTrue(stderr().contains("Failed to save phone book:"));
    }

    @Test
    void main_handles_noArgs_and_withArgs(@TempDir Path tempDir) {
        captureStdout();

        setAppInput("count", "unknown", "exit");
        App.main(new String[]{});
        assertTrue(stdout().contains("The Phone Book has 0 records."));

        out.reset();
        PhoneBook pb = new PhoneBook();
        pb.add(new Person("Main", "Load"));
        Path db = tempDir.resolve("main.db");
        App.save(db.toString(), pb);

        setAppInput("exit");
        App.main(new String[]{db.toString()});
        assertTrue(stdout().contains("open " + db));
    }

    @Test
    void printInfo_forPerson_and_searchText() {
        captureStdout();
        Person p = new Person("Neo", "Anderson");
        p.setFieldValue("birth", "1964-03-11");
        p.setFieldValue("gender", "M");
        p.setFieldValue("number", "101-22-33");

        String searchText = p.getSearchText();
        assertTrue(searchText.contains("Neo"));
        assertTrue(searchText.contains("Anderson"));
        assertTrue(searchText.contains("101-22-33"));

        p.printInfo();
        List<String> fields = p.getEditableFields();
        assertEquals(Arrays.asList("name", "surname", "birth", "gender", "number"), fields);
        assertTrue(stdout().contains("Name: Neo"));
    }
}
