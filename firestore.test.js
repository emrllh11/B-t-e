const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const EMRULLAH_UID = "user_emrullah_1";
const ESIM_UID = "user_esim_2";
const STRANGER_UID = "user_stranger_3";

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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated read/write to familyBudgets fails", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("familyBudgets").doc("fam_1").set({
      id: "fam_1",
      name: "Sağlam Ailesi",
      familyCode: "SAGLAM26",
      passcode: "1234",
      memberUids: [EMRULLAH_UID],
      spouse1Name: "Emrullah",
      spouse2Name: "Eşim"
    })
  );
});

test("Authenticated spouse can create and read their family budget", async () => {
  const emrullahDb = testEnv.authenticatedContext(EMRULLAH_UID).firestore();
  await assertSucceeds(
    emrullahDb.collection("familyBudgets").doc("fam_1").set({
      id: "fam_1",
      name: "Sağlam Ailesi",
      familyCode: "SAGLAM26",
      passcode: "1234",
      memberUids: [EMRULLAH_UID, ESIM_UID],
      spouse1Name: "Emrullah",
      spouse2Name: "Eşim"
    })
  );

  // Both spouses can read
  const esimDb = testEnv.authenticatedContext(ESIM_UID).firestore();
  await assertSucceeds(esimDb.collection("familyBudgets").doc("fam_1").get());

  // Stranger cannot read the budget document directly
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  await assertFails(strangerDb.collection("familyBudgets").doc("fam_1").get());
});

test("Spouse in memberUids can create and read shared transactions", async () => {
  // Setup family document
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("familyBudgets").doc("fam_1").set({
      id: "fam_1",
      name: "Sağlam Ailesi",
      familyCode: "SAGLAM26",
      passcode: "1234",
      memberUids: [EMRULLAH_UID, ESIM_UID],
      spouse1Name: "Emrullah",
      spouse2Name: "Eşim"
    });
  });

  const emrullahDb = testEnv.authenticatedContext(EMRULLAH_UID).firestore();
  const esimDb = testEnv.authenticatedContext(ESIM_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();

  // Emrullah adds transaction
  await assertSucceeds(
    emrullahDb.collection("familyBudgets").doc("fam_1").collection("transactions").doc("tx_1").set({
      id: "tx_1",
      familyId: "fam_1",
      title: "Market Alışverişi",
      amount: 450.0,
      type: "EXPENSE",
      category: "MARKET",
      spender: "Emrullah",
      timestamp: Date.now()
    })
  );

  // Eşim can read and update
  await assertSucceeds(
    esimDb.collection("familyBudgets").doc("fam_1").collection("transactions").doc("tx_1").get()
  );

  // Stranger cannot read or write to the family transactions
  await assertFails(
    strangerDb.collection("familyBudgets").doc("fam_1").collection("transactions").doc("tx_1").get()
  );
  await assertFails(
    strangerDb.collection("familyBudgets").doc("fam_1").collection("transactions").doc("tx_2").set({
      id: "tx_2",
      familyId: "fam_1",
      title: "Hacker Harcama",
      amount: 100.0,
      type: "EXPENSE",
      category: "OTHER",
      spender: "Stranger",
      timestamp: Date.now()
    })
  );
});
