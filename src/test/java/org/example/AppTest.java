package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    private void captureStdout() {
        System.setOut(new PrintStream(out));
    }

    private String stdout() {
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
        out.reset();
        // clean up global state used by App.searchIndexes
        App.book = null;
        App.filePath = null;
    }

    // -------------------------
    // PhoneBook basics
    // -------------------------

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

        pb.remove(0);
        assertEquals(1, pb.count());
        assertSame(o, pb.get(0));
    }

    // -------------------------
    // Record number validation
    // -------------------------

    @Test
    void setNumber_acceptsValidFormats_andSetsNumber() {
        Person p = new Person("A", "B");

        p.setFieldValue("number", "+1 (234) 567-890"); // valid by your rules
        assertTrue(p.hasNumber());
        assertEquals("+1 (234) 567-890", p.getNumber());

        p.setFieldValue("number", "123-45-67"); // groups: 123 / 45 / 67 (ok: first >=1, others >=2)
        assertTrue(p.hasNumber());
        assertEquals("123-45-67", p.getNumber());

        p.setFieldValue("number", "a1b2"); // letters/digits ok
        assertTrue(p.hasNumber());
        assertEquals("a1b2", p.getNumber());
    }

    @Test
    void setNumber_rejectsInvalidFormats_printsMessage_andClearsNumber() {
        captureStdout();
        Person p = new Person("A", "B");

        p.setFieldValue("number", "12"); // invalid: only one group, but that's actually ok (first group len>=1)
        // pick a definitely invalid one: has paren mismatch/extra parens or illegal chars
        p.setFieldValue("number", "(12)(34)"); // invalid: second paren group not allowed by idx > 1 / multiple
        assertFalse(p.hasNumber());
        assertEquals("", p.getNumber());
        assertTrue(stdout().contains("Wrong number format!"));
    }

    @Test
    void setNumber_emptyOrNull_clearsNumber_noError() {
        captureStdout();
        Person p = new Person("A", "B");

        p.setFieldValue("number", "123 456");
        assertTrue(p.hasNumber());

        p.setFieldValue("number", "");
        assertFalse(p.hasNumber());
        assertEquals("", p.getNumber());

        // via setFieldValue we can only pass String, but behavior for null is in setNumber:
        p.setNumber(null);
        assertFalse(p.hasNumber());
        assertEquals("", p.getNumber());

        // should not print "Wrong number format!" for empty/null
        assertFalse(stdout().contains("Wrong number format!"));
    }

    // -------------------------
    // Person birth / gender validation
    // -------------------------

    @Test
    void person_birthDate_validStored_invalidClearedAndPrints() {
        captureStdout();
        Person p = new Person("John", "Smith");

        p.setFieldValue("birth", "2000-02-29");
        assertEquals("2000-02-29", p.getFieldValue("birth"));

        p.setFieldValue("birth", "not-a-date");
        assertEquals("[no data]", p.getFieldValue("birth"));
        assertTrue(stdout().contains("Bad birth date!"));

        out.reset();
        p.setFieldValue("birth", "");
        assertEquals("[no data]", p.getFieldValue("birth"));
        assertTrue(stdout().contains("Bad birth date!"));
    }

    @Test
    void person_gender_onlyMOrF_elseClearedAndPrints() {
        captureStdout();
        Person p = new Person("Jane", "Doe");

        p.setFieldValue("gender", "F");
        assertEquals("F", p.getFieldValue("gender"));

        p.setFieldValue("gender", "X");
        assertEquals("[no data]", p.getFieldValue("gender"));
        assertTrue(stdout().contains("Bad gender!"));

        out.reset();
        p.setFieldValue("gender", "");
        assertEquals("[no data]", p.getFieldValue("gender"));
        assertTrue(stdout().contains("Bad gender!"));
    }

    // -------------------------
    // searchIndexes() logic
    // -------------------------

    @Test
    void searchIndexes_findsMatches_usingRegex_caseInsensitive() {
        PhoneBook pb = new PhoneBook();
        Person p1 = new Person("John", "Smith");
        p1.setFieldValue("number", "123 456");
        Person p2 = new Person("Alice", "Johnson");
        Organization o1 = new Organization("OpenAI", "SF");

        pb.add(p1);
        pb.add(p2);
        pb.add(o1);

        App.book = pb;

        // Regex: "john" should match John Smith and Alice Johnson (surname contains john)
        List<Integer> res = App.searchIndexes("john");
        assertEquals(Arrays.asList(0, 1), res);

        // Regex: "open.*" should match OpenAI
        assertEquals(Collections.singletonList(2), App.searchIndexes("open.*"));
    }

    @Test
    void searchIndexes_invalidRegex_fallsBackToLiteralMatch() {
        PhoneBook pb = new PhoneBook();
        Person p = new Person("A", "B");
        p.setFieldValue("number", "123");

        pb.add(p);
        App.book = pb;

        // invalid regex (unclosed bracket) falls back to literal search.
        // Current record text contains "[no data]" fields, so "[" matches.
        assertEquals(Collections.singletonList(0), App.searchIndexes("["));

        // But if we search for a literal string that DOES exist and is a regex-special pattern,
        // fallback should still work. E.g. search for "A B" with a plus or parens in query:
        assertEquals(Collections.singletonList(0), App.searchIndexes("A"));
        assertEquals(Collections.singletonList(0), App.searchIndexes("B"));
    }

    // -------------------------
    // save/load serialization
    // -------------------------

    @Test
    void saveThenLoad_roundTrip_preservesRecords(@TempDir Path tempDir) {
        Path file = tempDir.resolve("phonebook.db");

        PhoneBook pb = new PhoneBook();
        Person p = new Person("John", "Smith");
        p.setFieldValue("birth", "1999-01-01");
        p.setFieldValue("gender", "M");
        p.setFieldValue("number", "+1 234 567 890");

        Organization o = new Organization("Acme", "NYC");
        o.setFieldValue("number", "111-22-33");

        pb.add(p);
        pb.add(o);

        App.save(file.toString(), pb);
        PhoneBook loaded = App.load(file.toString());

        assertEquals(2, loaded.count());

        Record r0 = loaded.get(0);
        Record r1 = loaded.get(1);

        assertTrue(r0 instanceof Person);
        assertTrue(r1 instanceof Organization);

        assertEquals("John Smith", r0.getListName());
        assertEquals("Acme", r1.getListName());

        assertEquals("1999-01-01", r0.getFieldValue("birth"));
        assertEquals("M", r0.getFieldValue("gender"));
        assertEquals("+1 234 567 890", r0.getNumber());

        assertEquals("NYC", r1.getFieldValue("address"));
        assertEquals("111-22-33", r1.getNumber());
    }

    @Test
    void load_nonExistentFile_returnsEmptyPhoneBook(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("does-not-exist.bin");
        PhoneBook loaded = App.load(missing.toString());
        assertNotNull(loaded);
        assertEquals(0, loaded.count());
    }
}
