/**
 * Tests pour l'interface web du Projet_reseau_2016
 * Teste le serveur HTTP et WebSocket
 *
 * Prerequis: Le serveur Java doit etre demarre (java Main)
 * Execution: node tests_web.js
 */

const http = require('http');
const crypto = require('crypto');

const TESTS_RESULTS = { passed: 0, failed: 0 };
const SERVER_HOST = '127.0.0.1';
const SERVER_PORT = 6111;

/**
 * Assertion avec log.
 * @param {string} description Description du test
 * @param {boolean} condition Resultat
 */
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
 * Effectue une requete HTTP GET.
 * @param {string} path Chemin
 * @returns {Promise<{statusCode: number, body: string, headers: Object}>}
 */
function httpGet(path) {
    return new Promise((resolve, reject) => {
        const options = {
            hostname: SERVER_HOST,
            port: SERVER_PORT,
            path: path,
            method: 'GET',
            timeout: 5000
        };

        const req = http.request(options, (res) => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', () => resolve({
                statusCode: res.statusCode,
                body: body,
                headers: res.headers
            }));
        });

        req.on('error', reject);
        req.on('timeout', () => {
            req.destroy();
            reject(new Error('Timeout'));
        });
        req.end();
    });
}

/**
 * Teste la connexion WebSocket (handshake uniquement).
 * @returns {Promise<{success: boolean, acceptKey: string}>}
 */
function testWebSocketHandshake() {
    return new Promise((resolve, reject) => {
        const key = crypto.randomBytes(16).toString('base64');

        const options = {
            hostname: SERVER_HOST,
            port: SERVER_PORT,
            path: '/',
            method: 'GET',
            headers: {
                'Upgrade': 'websocket',
                'Connection': 'Upgrade',
                'Sec-WebSocket-Key': key,
                'Sec-WebSocket-Version': '13'
            },
            timeout: 5000
        };

        const req = http.request(options, (res) => {
            const success = res.statusCode === 101;
            const acceptKey = res.headers['sec-websocket-accept'] || '';
            resolve({ success, acceptKey, statusCode: res.statusCode });
        });

        req.on('error', reject);
        req.on('timeout', () => {
            req.destroy();
            reject(new Error('Timeout'));
        });
        req.end();
    });
}

/**
 * Verifie si le serveur est accessible.
 * @returns {Promise<boolean>}
 */
async function isServerRunning() {
    try {
        await httpGet('/');
        return true;
    } catch (e) {
        return false;
    }
}

// ==================== TESTS ====================

async function runTests() {
    console.log("=== Tests Interface Web Projet_reseau_2016 ===\n");

    // Verifier si le serveur est demarre
    const serverRunning = await isServerRunning();

    if (!serverRunning) {
        console.log("[SKIP] Serveur non demarre. Demarrez avec: cd .. && javac *.java && java Main");
        console.log("\n=== Tests hors-ligne (sans serveur) ===\n");
        runOfflineTests();
        return;
    }

    console.log("[INFO] Serveur detecte sur http://" + SERVER_HOST + ":" + SERVER_PORT + "\n");

    // Tests HTTP
    try {
        const indexResponse = await httpGet('/');
        assert("GET / retourne 200", indexResponse.statusCode === 200);
        assert("GET / contient HTML", indexResponse.body.includes('<!DOCTYPE html>'));
        assert("GET / contient titre", indexResponse.body.includes('Systeme d'));
    } catch (e) {
        assert("GET / accessible", false);
    }

    try {
        const cssResponse = await httpGet('/style.css');
        assert("GET /style.css retourne 200", cssResponse.statusCode === 200);
        assert("GET /style.css Content-Type CSS", cssResponse.headers['content-type'].includes('text/css'));
    } catch (e) {
        assert("GET /style.css accessible", false);
    }

    try {
        const jsResponse = await httpGet('/client.js');
        assert("GET /client.js retourne 200", jsResponse.statusCode === 200);
        assert("GET /client.js Content-Type JS", jsResponse.headers['content-type'].includes('javascript'));
    } catch (e) {
        assert("GET /client.js accessible", false);
    }

    try {
        const notFoundResponse = await httpGet('/fichier_inexistant.xyz');
        assert("GET /fichier_inexistant retourne 404", notFoundResponse.statusCode === 404);
    } catch (e) {
        assert("GET /fichier_inexistant gere", false);
    }

    // Test WebSocket handshake
    try {
        const wsResult = await testWebSocketHandshake();
        assert("WebSocket handshake 101 Switching Protocols", wsResult.success);
        assert("WebSocket Sec-WebSocket-Accept present", wsResult.acceptKey.length > 0);
    } catch (e) {
        assert("WebSocket handshake reussi", false);
    }

    printSummary();
}

/**
 * Tests hors-ligne (validation code client.js).
 */
function runOfflineTests() {
    const fs = require('fs');
    const path = require('path');

    // Lire client.js
    const clientJsPath = path.join(__dirname, '..', 'WEB', 'client.js');
    let clientJs = '';

    try {
        clientJs = fs.readFileSync(clientJsPath, 'utf8');
        assert("client.js existe", true);
    } catch (e) {
        assert("client.js existe", false);
        printSummary();
        return;
    }

    // Verifier fonctions requises
    assert("client.js contient connect()", clientJs.includes('function connect()'));
    assert("client.js contient drawAnneau()", clientJs.includes('function drawAnneau()'));
    assert("client.js contient handleMessage()", clientJs.includes('function handleMessage('));
    assert("client.js contient sendCommand()", clientJs.includes('function sendCommand('));
    assert("client.js contient addEntite()", clientJs.includes('function addEntite()'));
    assert("client.js contient removeEntite()", clientJs.includes('function removeEntite()'));
    assert("client.js contient updateEntitesList()", clientJs.includes('function updateEntitesList()'));
    assert("client.js contient addLog()", clientJs.includes('function addLog('));

    // Lire index.html
    const indexPath = path.join(__dirname, '..', 'WEB', 'index.html');
    let indexHtml = '';

    try {
        indexHtml = fs.readFileSync(indexPath, 'utf8');
        assert("index.html existe", true);
    } catch (e) {
        assert("index.html existe", false);
        printSummary();
        return;
    }

    assert("index.html contient canvas", indexHtml.includes('<canvas'));
    assert("index.html contient bouton addEntite", indexHtml.includes('addEntite()'));
    assert("index.html contient bouton WHOS", indexHtml.includes('sendWHOS'));
    assert("index.html lie client.js", indexHtml.includes('client.js'));
    assert("index.html lie style.css", indexHtml.includes('style.css'));

    // Lire style.css
    const cssPath = path.join(__dirname, '..', 'WEB', 'style.css');
    let styleCss = '';

    try {
        styleCss = fs.readFileSync(cssPath, 'utf8');
        assert("style.css existe", true);
    } catch (e) {
        assert("style.css existe", false);
        printSummary();
        return;
    }

    assert("style.css contient body", styleCss.includes('body'));
    assert("style.css contient canvas", styleCss.includes('canvas') || styleCss.includes('#anneau'));

    printSummary();
}

/**
 * Affiche le resume des tests.
 */
function printSummary() {
    console.log("\n=== Resume ===");
    console.log(`Tests passes: ${TESTS_RESULTS.passed}`);
    console.log(`Tests echoues: ${TESTS_RESULTS.failed}`);
    console.log(`Total: ${TESTS_RESULTS.passed + TESTS_RESULTS.failed}`);

    if (TESTS_RESULTS.failed > 0) {
        process.exit(1);
    }
}

// Executer les tests
runTests().catch(e => {
    console.error("Erreur fatale:", e.message);
    process.exit(1);
});
