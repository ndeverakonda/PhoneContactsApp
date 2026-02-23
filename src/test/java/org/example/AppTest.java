package org.example;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    @TempDir
    Path tempDir;

    private PrintStream originalOut;
    private InputStream originalIn;

    @BeforeEach
    void setUp() throws Exception {
        originalOut = System.out;
        originalIn = System.in;

        App.contacts = new ArrayList<>();
        App.fileName = null;

        setAppScannerWithInput("");
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    // ----------------------------
    // Low-level unit coverage
    // ----------------------------

    @Test
    void isValidNumber_acceptsCommonFormats_andRejectsBad() {
        assertTrue(Contact.isValidNumber("123"));
        assertTrue(Contact.isValidNumber("+0 (123) 456-789-9999"));
        assertTrue(Contact.isValidNumber("(123) 234 345-456"));
        assertTrue(Contact.isValidNumber("+123 12 34 56"));
        assertTrue(Contact.isValidNumber("a1-b2"));

        assertFalse(Contact.isValidNumber("++123"));
        assertFalse(Contact.isValidNumber(")("));
        assertFalse(Contact.isValidNumber("")); // matches() on empty should be false
    }

    @Test
    void getNumberPrintable_branches() {
        PersonContact p = new PersonContact("A", "B", "2000-01-01", "M", "123");
        assertEquals("123", p.getNumberPrintable());

        p.setNumber("");
        assertEquals("[no number]", p.getNumberPrintable());

        p.setNumber(null);
        assertEquals("[no number]", p.getNumberPrintable());
    }

    @Test
    void setNumber_invalid_printsMessage_andClearsNumber() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        OrganizationContact org = new OrganizationContact("Org", "Addr", "123");
        org.setNumber("++123"); // invalid

        assertEquals("[no number]", org.getNumberPrintable());
        assertTrue(out.toString().contains("Wrong number format!"));
    }

    @Test
    void person_birth_and_gender_all_branches_plus_switches() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        PersonContact p = new PersonContact("John", "Smith", "", "", "123");

        // birth: empty -> [no data]
        assertEquals("[no data]", p.getFieldValue("birth"));
        // gender: empty -> [no data]
        assertEquals("[no data]", p.getFieldValue("gender"));

        // invalid birth
        p.setBirthDate("nope");
        assertEquals("[no data]", p.getFieldValue("birth"));

        // valid birth
        p.setBirthDate("1999-12-31");
        assertEquals("1999-12-31", p.getFieldValue("birth"));

        // invalid gender
        p.setGender("x");
        assertEquals("[no data]", p.getFieldValue("gender"));

        // valid gender with lower-case input
        p.setGender("f");
        assertEquals("F", p.getFieldValue("gender"));

        // setField switch coverage
        p.setField("name", "A");
        p.setField("surname", "B");
        p.setField("birth", "2001-01-01");
        p.setField("gender", "M");
        p.setField("number", "555");
        p.setField("unknown", "zzz"); // default

        assertEquals("A", p.getFieldValue("name"));
        assertEquals("B", p.getFieldValue("surname"));
        assertEquals("2001-01-01", p.getFieldValue("birth"));
        assertEquals("M", p.getFieldValue("gender"));
        assertEquals("555", p.getFieldValue("number"));
        assertEquals("", p.getFieldValue("unknown"));

        String printed = out.toString();
        assertTrue(printed.contains("Bad birth date!"));
        assertTrue(printed.contains("Bad gender!"));
    }

    @Test
    void organization_switches_and_printInfo_lines() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        OrganizationContact o = new OrganizationContact("Acme", "Delhi", "999");
        o.printInfo(); // line coverage

        // setField switch coverage
        o.setField("name", "OpenAI");
        o.setField("address", "SF");
        o.setField("number", "123");
        o.setField("unknown", "zzz"); // default

        assertEquals("OpenAI", o.getFieldValue("name"));
        assertEquals("SF", o.getFieldValue("address"));
        assertEquals("123", o.getFieldValue("number"));
        assertEquals("", o.getFieldValue("unknown"));

        assertTrue(out.toString().contains("Organization name:"));
    }

    // ----------------------------
    // Save/load coverage
    // ----------------------------

    @Test
    void save_whenFileNameNull_returnsImmediately() {
        App.fileName = null;
        App.contacts.add(new PersonContact("A", "B", "2000-01-01", "M", "123"));
        assertDoesNotThrow(App::save);
    }

    @Test
    void load_whenMissing_createsEmptyFile_andEmptyList() {
        File f = tempDir.resolve("phonebook.db").toFile();
        assertFalse(f.exists());

        App.fileName = f.getAbsolutePath();
        App.load();

        assertNotNull(App.contacts);
        assertEquals(0, App.contacts.size());
        assertTrue(f.exists());
    }

    @Test
    void save_then_load_roundTrip() {
        File f = tempDir.resolve("pb.db").toFile();
        App.fileName = f.getAbsolutePath();

        App.contacts.add(new PersonContact("John", "Smith", "1999-12-31", "M", "123"));
        App.contacts.add(new OrganizationContact("Org", "Addr", "999"));

        App.save();
        App.contacts = new ArrayList<>();
        App.load();

        assertEquals(2, App.contacts.size());
        assertEquals("John Smith", App.contacts.get(0).getListName());
        assertEquals("Org", App.contacts.get(1).getListName());
    }

    @Test
    void load_withCorruptFile_fallsBackToEmpty() throws IOException {
        File f = tempDir.resolve("corrupt.db").toFile();
        App.fileName = f.getAbsolutePath();

        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(new byte[]{1, 2, 3, 4, 5});
        }

        App.load();
        assertNotNull(App.contacts);
        assertEquals(0, App.contacts.size());
    }

    @Test
    void printSavedIfFile_branches() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        App.fileName = null;
        App.printSavedIfFile();
        assertEquals("", out.toString());

        App.fileName = tempDir.resolve("x.db").toString();
        App.printSavedIfFile();
        assertTrue(out.toString().contains("Saved"));
    }

    // ----------------------------
    // Drive PRIVATE interactive flows (big branch wins)
    // ----------------------------

    @Test
    void add_person_and_organization_and_unknownType() throws Exception {
        // Use a real file so save() runs (and "Saved" branch can happen)
        File f = tempDir.resolve("data.db").toFile();
        App.fileName = f.getAbsolutePath();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        // 1) person
        setAppScannerWithInput(
                "person\n" +
                        "John\n" +
                        "Smith\n" +
                        "1999-12-31\n" +
                        "M\n" +
                        "123\n"
        );
        invokePrivateStatic("add");

        // 2) organization
        setAppScannerWithInput(
                "organization\n" +
                        "Acme\n" +
                        "Delhi\n" +
                        "999\n"
        );
        invokePrivateStatic("add");

        // 3) unknown -> should do nothing (covers add() falling through)
        setAppScannerWithInput("something-else\n");
        invokePrivateStatic("add");

        assertEquals(2, App.contacts.size());
        assertTrue(out.toString().contains("The record added."));
        assertTrue(out.toString().contains("Saved"));
    }

    @Test
    void listMenu_back_branch_and_invalidInput_loop_then_selectRecord_then_menu() throws Exception {
        App.contacts.add(new PersonContact("John", "Smith", "1999-12-31", "M", "123"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        // First call: just "back"
        setAppScannerWithInput("back\n");
        invokePrivateStatic("listMenu");
        assertTrue(out.toString().contains("1. John Smith"));

        // Second call: invalid -> still loops -> valid "1" -> inside recordMenu -> choose "menu"
        out.reset();
        setAppScannerWithInput(
                "abc\n" +   // not a number, not back
                        "0\n" +     // number but invalid idx
                        "1\n" +     // valid -> recordMenu
                        "menu\n"    // exit recordMenu
        );
        invokePrivateStatic("listMenu");
        assertTrue(out.toString().contains("[record] Enter action"));
    }

    @Test
    void searchMenu_again_branch_selectRecord_then_menu_and_back_branch() throws Exception {
        App.contacts.add(new PersonContact("John", "Smith", "1999-12-31", "M", "123"));
        App.contacts.add(new OrganizationContact("Acme", "Delhi", "999"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        // searchMenu flow:
        // doSearchOnce: query "john" -> 1 result
        // then cmd "again" -> new query "acme" -> 1 result
        // then cmd "1" -> open recordMenu -> "menu"
        setAppScannerWithInput(
                "john\n" +
                        "again\n" +
                        "acme\n" +
                        "1\n" +
                        "menu\n"
        );
        invokePrivateStatic("searchMenu");
        assertTrue(out.toString().contains("Found 1 results:"));
        assertTrue(out.toString().toLowerCase(Locale.ROOT).contains("acme"));

        // Also cover searchMenu "back" branch quickly
        out.reset();
        setAppScannerWithInput(
                "john\n" +
                        "back\n"
        );
        invokePrivateStatic("searchMenu");
        assertTrue(out.toString().contains("[search] Enter action"));
    }

    @Test
    void recordMenu_edit_branch_then_delete_branch() throws Exception {
        File f = tempDir.resolve("records.db").toFile();
        App.fileName = f.getAbsolutePath();

        App.contacts.add(new PersonContact("John", "Smith", "1999-12-31", "M", "123"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        // recordMenu:
        // edit -> choose field "name" -> set "Jane"
        // then delete
        setAppScannerWithInput(
                "edit\n" +
                        "name\n" +
                        "Jane\n" +
                        "delete\n"
        );
        invokePrivateStatic("recordMenu", int.class, 0);

        assertEquals(0, App.contacts.size());
        String printed = out.toString();
        assertTrue(printed.contains("The record updated!"));
        assertTrue(printed.contains("The record removed!"));
        assertTrue(printed.contains("Saved"));
    }

    @Test
    void doSearchOnce_patternSyntaxException_branch() throws Exception {
        App.contacts.add(new OrganizationContact("OpenAI", "San Francisco", "999"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        // bad regex -> PatternSyntaxException -> Pattern.quote branch
        setAppScannerWithInput("[\n");
        @SuppressWarnings("unchecked")
        List<Integer> results = (List<Integer>) invokePrivateStatic("doSearchOnce");

        assertEquals(0, results.size());
        assertTrue(out.toString().contains("Found 0 results:"));
    }

    @Test
    void count_prints_correct_size() throws Exception {
        App.contacts.add(new PersonContact("A", "B", "2000-01-01", "M", "123"));
        App.contacts.add(new OrganizationContact("Org", "Addr", "999"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        invokePrivateStatic("count");
        assertTrue(out.toString().contains("The Phone Book has 2 records."));
    }

    // ----------------------------
    // Run main() to cover top-level switch + default + exit
    // ----------------------------

    @Test
    void main_drives_menu_switch_default_and_exit() {
        // Script:
        // unknown action -> default branch
        // count -> prints 0
        // exit -> return
        setAppScannerWithInput(
                "whatever\n" +
                        "count\n" +
                        "exit\n"
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        assertDoesNotThrow(() -> App.main(new String[]{}));

        String printed = out.toString();
        assertTrue(printed.contains("[menu] Enter action"));
        assertTrue(printed.contains("The Phone Book has 0 records."));
    }

    // ----------------------------
    // isNumber helper (private)
    // ----------------------------

    @Test
    void isNumber_privateHelper_more_branches() throws Exception {
        assertFalse((boolean) invokePrivateStatic("isNumber", String.class, (Object) null));
        assertFalse((boolean) invokePrivateStatic("isNumber", String.class, ""));
        assertFalse((boolean) invokePrivateStatic("isNumber", String.class, " 123")); // space
        assertFalse((boolean) invokePrivateStatic("isNumber", String.class, "12a3"));
        assertTrue((boolean) invokePrivateStatic("isNumber", String.class, "0"));
        assertTrue((boolean) invokePrivateStatic("isNumber", String.class, "123"));
    }

    // ----------------------------
    // Helpers
    // ----------------------------

    private void setAppScannerWithInput(String input) {
        // Your compiled class allows this (as seen in your decompiled output)
        App.sc = new Scanner(new ByteArrayInputStream(input.getBytes()));
    }

    private static Object invokePrivateStatic(String methodName, Object... args) throws Exception {
        Method target = null;
        for (Method m : App.class.getDeclaredMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == args.length) {
                target = m;
                break;
            }
        }
        assertNotNull(target, "Method not found: " + methodName);
        target.setAccessible(true);
        return target.invoke(null, args);
    }

    private static Object invokePrivateStatic(String methodName, Class<?> p1, Object a1) throws Exception {
        Method m = App.class.getDeclaredMethod(methodName, p1);
        m.setAccessible(true);
        return m.invoke(null, a1);
    }
}