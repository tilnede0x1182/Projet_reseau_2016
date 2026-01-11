/**
 * Client WebSocket pour l'interface de visualisation de l'anneau.
 */

let ws = null;
let entites = [];
let canvas = null;
let ctx = null;

/**
 * Initialisation au chargement de la page.
 */
document.addEventListener('DOMContentLoaded', function() {
    canvas = document.getElementById('anneau-canvas');
    ctx = canvas.getContext('2d');

    connect();
    drawAnneau();
});

/**
 * Connexion au serveur WebSocket.
 */
function connect() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = protocol + '//' + window.location.host;

    console.log('Connexion a ' + wsUrl);
    ws = new WebSocket(wsUrl);

    ws.onopen = function() {
        console.log('WebSocket connecte');
        updateConnectionStatus(true);
        addLog('Connecte au serveur', 'log');
    };

    ws.onclose = function() {
        console.log('WebSocket deconnecte');
        updateConnectionStatus(false);
        addLog('Deconnecte du serveur', 'error');

        setTimeout(connect, 3000);
    };

    ws.onerror = function(error) {
        console.error('WebSocket erreur:', error);
        addLog('Erreur de connexion', 'error');
    };

    ws.onmessage = function(event) {
        try {
            const data = JSON.parse(event.data);
            handleMessage(data);
        } catch (e) {
            console.error('Erreur parsing JSON:', e);
        }
    };
}

/**
 * Met a jour le statut de connexion.
 * @param {boolean} connected Etat de connexion
 */
function updateConnectionStatus(connected) {
    const statusEl = document.getElementById('connection-status');
    if (connected) {
        statusEl.textContent = 'Connecte';
        statusEl.className = 'connected';
    } else {
        statusEl.textContent = 'Deconnecte';
        statusEl.className = 'disconnected';
    }
}

/**
 * Gere un message recu du serveur.
 * @param {Object} data Donnees JSON
 */
function handleMessage(data) {
    switch (data.type) {
        case 'state':
            entites = data.entites || [];
            updateEntitesList();
            drawAnneau();
            break;

        case 'log':
            addLog(data.message, 'log');
            break;

        case 'message':
            const msg = data.entite + ' -> ' + data.messageType + ': ' + data.content;
            addLog(msg, 'message');
            break;

        case 'error':
            addLog('Erreur: ' + data.message, 'error');
            break;

        default:
            console.log('Message inconnu:', data);
    }
}

/**
 * Met a jour la liste des entites dans le DOM.
 */
function updateEntitesList() {
    const listEl = document.getElementById('entites-list');
    const countEl = document.getElementById('entites-count');

    countEl.textContent = entites.length;
    listEl.innerHTML = '';

    entites.forEach(function(entite) {
        const li = document.createElement('li');

        const infoDiv = document.createElement('div');
        const idSpan = document.createElement('span');
        idSpan.className = 'entite-id';
        idSpan.textContent = entite.id;

        const detailSpan = document.createElement('span');
        detailSpan.className = 'entite-info';
        detailSpan.textContent = ' UDP:' + entite.portUDP + ' TCP:' + entite.portTCP;

        infoDiv.appendChild(idSpan);
        infoDiv.appendChild(detailSpan);

        const removeBtn = document.createElement('button');
        removeBtn.textContent = 'X';
        removeBtn.className = 'danger';
        removeBtn.onclick = function() {
            removeEntiteById(entite.id);
        };

        li.appendChild(infoDiv);
        li.appendChild(removeBtn);
        listEl.appendChild(li);
    });
}

/**
 * Dessine l'anneau sur le canvas.
 */
function drawAnneau() {
    const width = canvas.width;
    const height = canvas.height;
    const centerX = width / 2;
    const centerY = height / 2;
    const radius = Math.min(width, height) / 2 - 60;
    const nodeRadius = 25;

    ctx.clearRect(0, 0, width, height);

    ctx.fillStyle = '#0f3460';
    ctx.fillRect(0, 0, width, height);

    if (entites.length === 0) {
        ctx.fillStyle = '#666';
        ctx.font = '16px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('Aucune entite', centerX, centerY);
        return;
    }

    ctx.strokeStyle = '#0f3460';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.arc(centerX, centerY, radius, 0, Math.PI * 2);
    ctx.stroke();

    const angleStep = (Math.PI * 2) / entites.length;

    for (let i = 0; i < entites.length; i++) {
        const angle = i * angleStep - Math.PI / 2;
        const nextAngle = ((i + 1) % entites.length) * angleStep - Math.PI / 2;

        const x1 = centerX + radius * Math.cos(angle);
        const y1 = centerY + radius * Math.sin(angle);
        const x2 = centerX + radius * Math.cos(nextAngle);
        const y2 = centerY + radius * Math.sin(nextAngle);

        drawArrow(x1, y1, x2, y2, nodeRadius);
    }

    for (let i = 0; i < entites.length; i++) {
        const entite = entites[i];
        const angle = i * angleStep - Math.PI / 2;
        const x = centerX + radius * Math.cos(angle);
        const y = centerY + radius * Math.sin(angle);

        ctx.beginPath();
        ctx.arc(x, y, nodeRadius, 0, Math.PI * 2);
        ctx.fillStyle = '#e94560';
        ctx.fill();
        ctx.strokeStyle = '#fff';
        ctx.lineWidth = 2;
        ctx.stroke();

        ctx.fillStyle = '#fff';
        ctx.font = 'bold 12px sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(entite.id, x, y);

        ctx.fillStyle = '#aaa';
        ctx.font = '10px sans-serif';
        ctx.fillText(':' + entite.portUDP, x, y + nodeRadius + 12);
    }
}

/**
 * Dessine une fleche entre deux points.
 * @param {number} x1 X depart
 * @param {number} y1 Y depart
 * @param {number} x2 X arrivee
 * @param {number} y2 Y arrivee
 * @param {number} nodeRadius Rayon des noeuds
 */
function drawArrow(x1, y1, x2, y2, nodeRadius) {
    const angle = Math.atan2(y2 - y1, x2 - x1);
    const arrowLength = 10;

    const startX = x1 + nodeRadius * Math.cos(angle);
    const startY = y1 + nodeRadius * Math.sin(angle);
    const endX = x2 - nodeRadius * Math.cos(angle);
    const endY = y2 - nodeRadius * Math.sin(angle);

    ctx.strokeStyle = '#1a5f7a';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(startX, startY);
    ctx.lineTo(endX, endY);
    ctx.stroke();

    ctx.fillStyle = '#1a5f7a';
    ctx.beginPath();
    ctx.moveTo(endX, endY);
    ctx.lineTo(
        endX - arrowLength * Math.cos(angle - Math.PI / 6),
        endY - arrowLength * Math.sin(angle - Math.PI / 6)
    );
    ctx.lineTo(
        endX - arrowLength * Math.cos(angle + Math.PI / 6),
        endY - arrowLength * Math.sin(angle + Math.PI / 6)
    );
    ctx.closePath();
    ctx.fill();
}

/**
 * Ajoute une entree au log.
 * @param {string} message Message
 * @param {string} type Type (log, message, error)
 */
function addLog(message, type) {
    const logContainer = document.getElementById('log-container');

    const entry = document.createElement('div');
    entry.className = 'log-entry ' + type + '-type';

    const timestamp = document.createElement('span');
    timestamp.className = 'log-timestamp';
    const now = new Date();
    timestamp.textContent = now.toLocaleTimeString();

    entry.appendChild(timestamp);
    entry.appendChild(document.createTextNode(message));

    logContainer.appendChild(entry);
    logContainer.scrollTop = logContainer.scrollHeight;
}

/**
 * Efface le log.
 */
function clearLog() {
    document.getElementById('log-container').innerHTML = '';
}

/**
 * Envoie une commande au serveur.
 * @param {string} command Commande
 */
function sendCommand(command) {
    if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ command: command }));
    } else {
        addLog('Non connecte au serveur', 'error');
    }
}

/**
 * Ajoute une entite.
 */
function addEntite() {
    sendCommand('addEntite');
}

/**
 * Retire la derniere entite.
 */
function removeEntite() {
    if (entites.length > 0) {
        const lastEntite = entites[entites.length - 1];
        removeEntiteById(lastEntite.id);
    }
}

/**
 * Retire une entite par son ID.
 * @param {string} id Identifiant
 */
function removeEntiteById(id) {
    if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ command: 'removeEntite', id: id }));
    }
}

/**
 * Envoie un message APPL.
 */
function sendAppl() {
    const input = document.getElementById('appl-message');
    const message = input.value.trim();

    if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ command: 'sendAPPL', message: message }));
        input.value = '';
    }
}
