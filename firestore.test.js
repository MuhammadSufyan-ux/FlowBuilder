const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated users cannot read or write profiles or projects", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Hacker",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can create and update own profile, but cannot spoof another user or mutate createdAt", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Sufyan",
      subtitle: "Flowchart Architect",
      email: "sufyan@example.com",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      displayName: "Muhammad Sufyan",
      subtitle: "BSDS AI Journey",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject createdAt tamper
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject unknown ghost field
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Owner can CRUD projects with native nodes/edges lists; cross-user access is denied", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  const projectRef = alice.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1");
  await assertSucceeds(
    projectRef.set({
      userId: ALICE_UID,
      title: "Login Process",
      description: "Auth flow",
      category: "Software",
      favorite: true,
      gridStyle: "PLUS",
      snapToGrid: true,
      nodes: [{ id: "n1", shape: "OVAL", text: "Start", x: 100, y: 100 }],
      edges: [],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Owner list & partial update succeed
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertSucceeds(
    projectRef.update({
      title: "Updated Login Flow",
      favorite: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user read/list/write/delete denied
  const bobRef = bob.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1");
  await assertFails(bobRef.get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(bobRef.update({ title: "Hijacked", updatedAt: new Date(Date.now() - 500) }));
  await assertFails(bobRef.delete());

  // Invalid gridStyle enum rejected
  await assertFails(
    projectRef.update({
      gridStyle: "INVALID_GRID",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Future timestamp rejected
  await assertFails(
    projectRef.update({
      title: "Future",
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  await assertSucceeds(projectRef.delete());
});
