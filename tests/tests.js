/**
 * Tests pour Projet_reseau_2016
 * Validation du protocole de communication en anneau
 * Execution: node tests.js
 */

const TESTS_RESULTS = { passed: 0, failed: 0 };

function assert(description, condition) {
    if (condition) {
        console.log(`[PASS] ${description}`);
        TESTS_RESULTS.passed++;
    } else {
        console.log(`[FAIL] ${description}`);
        TESTS_RESULTS.failed++;
    }
}

/**
 * Types de messages supportes.
 */
const TYPES_MESSAGES = ["APPL", "WHOS", "MEMB", "GBYE", "EYBG", "TEST", "DOWN"];

/**
 * Genere un ID unique.
 * @returns {string} ID unique.
 */
function genererIdMessage() {
    return Date.now().toString(36) + Math.random().toString(36).substr(2, 5);
}

/**
 * Valide le format d'un message.
 * @param {Object} message Message a valider.
 * @returns {boolean} True si valide.
 */
function validerMessage(message) {
    if (!message.idm || !message.type || !message.expediteur) return false;
    if (!TYPES_MESSAGES.includes(message.type)) return false;
    return true;
}

/**
 * Cree un message.
 * @param {string} type Type de message.
 * @param {string} expediteur ID expediteur.
 * @param {string} contenu Contenu.
 * @returns {Object} Message cree.
 */
function creerMessage(type, expediteur, contenu = "") {
    return {
        idm: genererIdMessage(),
        type: type,
        expediteur: expediteur,
        contenu: contenu,
        timestamp: Date.now()
    };
}

/**
 * Simule une entite dans l'anneau.
 */
class Entite {
    constructor(id) {
        this.id = id;
        this.messagesRecus = [];
        this.messagesEnvoyes = [];
        this.suivant = null;
    }

    envoyerMessage(message) {
        this.messagesEnvoyes.push(message);
        if (this.suivant) {
            this.suivant.recevoirMessage(message);
        }
    }

    recevoirMessage(message) {
        // Eviter les doublons
        if (this.messagesRecus.find(m => m.idm === message.idm)) return;
        this.messagesRecus.push(message);
        // Relayer si pas l'expediteur
        if (message.expediteur !== this.id && this.suivant) {
            this.suivant.recevoirMessage(message);
        }
    }
}

// ==================== TESTS ====================

console.log("=== Tests Projet-reseau ===\n");

// Tests generation ID
assert("ID unique genere", genererIdMessage().length > 0);
assert("Deux IDs differents", genererIdMessage() !== genererIdMessage());

// Tests validation message
assert("Message valide", validerMessage({ idm: "123", type: "TEST", expediteur: "E1" }));
assert("Message sans idm invalide", !validerMessage({ type: "TEST", expediteur: "E1" }));
assert("Message type inconnu invalide", !validerMessage({ idm: "123", type: "UNKNOWN", expediteur: "E1" }));

// Tests creation message
assert("Message cree avec type", creerMessage("APPL", "E1").type === "APPL");
assert("Message cree avec expediteur", creerMessage("TEST", "E2").expediteur === "E2");

// Tests entite
assert("Entite envoie message", (() => {
    const entite = new Entite("E1");
    const message = creerMessage("TEST", "E1");
    entite.envoyerMessage(message);
    return entite.messagesEnvoyes.length === 1;
})());

assert("Anneau 3 entites relaye message", (() => {
    const entiteA = new Entite("A");
    const entiteB = new Entite("B");
    const entiteC = new Entite("C");
    entiteA.suivant = entiteB;
    entiteB.suivant = entiteC;
    entiteC.suivant = entiteA;

    const message = creerMessage("TEST", "A");
    entiteA.envoyerMessage(message);

    return entiteB.messagesRecus.length === 1 && entiteC.messagesRecus.length === 1;
})());

// ==================== RESUME ====================

console.log("\n=== Resume ===");
console.log(`Tests passes: ${TESTS_RESULTS.passed}`);
console.log(`Tests echoues: ${TESTS_RESULTS.failed}`);

if (TESTS_RESULTS.failed > 0) process.exit(1);
