const API_BASE = 'http://localhost:8090/api';

let currentScanId = null;
let statusCheckInterval = null;

// Initialisation
document.addEventListener('DOMContentLoaded', () => {
    initializeTabs();
    initializeScanForm();
    initializeHistory();
    checkHealth();
    setInterval(checkHealth, 30000); // Vérifier toutes les 30 secondes
});

// Gestion des onglets
function initializeTabs() {
    const tabButtons = document.querySelectorAll('.tab-button');
    const tabContents = document.querySelectorAll('.tab-content');

    tabButtons.forEach(button => {
        button.addEventListener('click', () => {
            const targetTab = button.getAttribute('data-tab');
            
            // Désactiver tous les onglets
            tabButtons.forEach(btn => btn.classList.remove('active'));
            tabContents.forEach(content => content.classList.remove('active'));
            
            // Activer l'onglet sélectionné
            button.classList.add('active');
            document.getElementById(targetTab + 'Tab').classList.add('active');
            
            // Charger le contenu si nécessaire
            if (targetTab === 'history') {
                loadHistory();
            } else if (targetTab === 'status') {
                checkHealth();
            }
        });
    });
}

// Formulaire de scan
function initializeScanForm() {
    const form = document.getElementById('scanForm');
    const cancelBtn = document.getElementById('cancelScanBtn');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        await createAndStartScan();
    });

    cancelBtn.addEventListener('click', async () => {
        if (currentScanId) {
            await cancelScan(currentScanId);
        }
    });
}

async function createAndStartScan() {
    try {
        const formData = {
            scannerName: document.getElementById('scannerName').value || null,
            options: {
                resolution: parseInt(document.getElementById('resolution').value),
                colorMode: document.getElementById('colorMode').value,
                pageSize: document.getElementById('pageSize').value
            }
        };

        // Créer la session
        const createResponse = await fetch(`${API_BASE}/scans`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });

        if (!createResponse.ok) {
            const error = await createResponse.json();
            showMessage('error', error.message || 'Erreur lors de la création du scan');
            return;
        }

        const scanResponse = await createResponse.json();
        currentScanId = scanResponse.scanId;

        // Démarrer le scan
        const startResponse = await fetch(`${API_BASE}/scans/${currentScanId}/start`, {
            method: 'POST'
        });

        if (!startResponse.ok) {
            const error = await startResponse.json();
            showMessage('error', error.message || 'Erreur lors du démarrage du scan');
            return;
        }

        // Afficher la progression
        document.getElementById('scanProgress').style.display = 'block';
        document.getElementById('scanForm').style.display = 'none';
        
        // Surveiller le statut
        monitorScan(currentScanId);

    } catch (error) {
        showMessage('error', 'Erreur: ' + error.message);
    }
}

async function monitorScan(scanId) {
    if (statusCheckInterval) {
        clearInterval(statusCheckInterval);
    }

    statusCheckInterval = setInterval(async () => {
        try {
            const response = await fetch(`${API_BASE}/scans/${scanId}`);
            if (!response.ok) return;

            const scan = await response.json();
            updateScanProgress(scan);

            if (scan.status === 'COMPLETED' || scan.status === 'FAILED' || scan.status === 'CANCELLED') {
                clearInterval(statusCheckInterval);
                statusCheckInterval = null;
                
                if (scan.status === 'COMPLETED') {
                    showMessage('success', 'Scan terminé avec succès!');
                    document.getElementById('scanStatusText').innerHTML = 
                        `Scan terminé. <a href="${API_BASE}/scans/${scanId}/pdf" target="_blank">Télécharger le PDF</a>`;
                } else if (scan.status === 'FAILED') {
                    showMessage('error', 'Scan échoué: ' + (scan.errorMessage || 'Erreur inconnue'));
                }
                
                setTimeout(() => {
                    document.getElementById('scanProgress').style.display = 'none';
                    document.getElementById('scanForm').style.display = 'block';
                    currentScanId = null;
                }, 5000);
            }
        } catch (error) {
            console.error('Erreur lors de la vérification du statut', error);
        }
    }, 2000);
}

function updateScanProgress(scan) {
    const statusText = document.getElementById('scanStatusText');
    const progressFill = document.getElementById('progressFill');
    
    statusText.textContent = `Statut: ${getStatusLabel(scan.status)}`;
    
    let progress = 0;
    if (scan.status === 'PENDING') progress = 10;
    else if (scan.status === 'RUNNING') progress = 50;
    else if (scan.status === 'COMPLETED') progress = 100;
    else if (scan.status === 'FAILED') progress = 0;
    
    progressFill.style.width = progress + '%';
}

function getStatusLabel(status) {
    const labels = {
        'PENDING': 'En attente',
        'RUNNING': 'En cours',
        'COMPLETED': 'Terminé',
        'FAILED': 'Échoué',
        'CANCELLED': 'Annulé'
    };
    return labels[status] || status;
}

async function cancelScan(scanId) {
    try {
        const response = await fetch(`${API_BASE}/scans/${scanId}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            const error = await response.json();
            showMessage('error', error.message || 'Erreur lors de l\'annulation');
            return;
        }

        showMessage('success', 'Scan annulé');
        if (statusCheckInterval) {
            clearInterval(statusCheckInterval);
            statusCheckInterval = null;
        }
    } catch (error) {
        showMessage('error', 'Erreur: ' + error.message);
    }
}

// Historique
function initializeHistory() {
    document.getElementById('refreshHistoryBtn').addEventListener('click', loadHistory);
    document.getElementById('statusFilter').addEventListener('change', loadHistory);
    loadHistory();
}

async function loadHistory() {
    try {
        const statusFilter = document.getElementById('statusFilter').value;
        const url = `${API_BASE}/scans?limit=50&offset=0${statusFilter ? '&status=' + statusFilter : ''}`;
        
        const response = await fetch(url);
        if (!response.ok) return;

        const data = await response.json();
        displayHistory(data.scans);
    } catch (error) {
        console.error('Erreur lors du chargement de l\'historique', error);
    }
}

function displayHistory(scans) {
    const list = document.getElementById('historyList');
    
    if (scans.length === 0) {
        list.innerHTML = '<p>Aucun scan dans l\'historique</p>';
        return;
    }

    list.innerHTML = scans.map(scan => `
        <div class="history-item">
            <div class="history-item-info">
                <div class="history-item-id">${scan.scanId}</div>
                <div>
                    <span class="history-item-status ${scan.status.toLowerCase()}">${getStatusLabel(scan.status)}</span>
                    <span style="margin-left: 10px; color: #666; font-size: 12px;">
                        ${new Date(scan.createdAt).toLocaleString('fr-FR')}
                    </span>
                </div>
            </div>
            <div class="history-item-actions">
                ${scan.status === 'COMPLETED' ? 
                    `<a href="${API_BASE}/scans/${scan.scanId}/pdf" target="_blank" class="btn btn-primary" style="text-decoration: none;">Télécharger</a>` : 
                    ''
                }
            </div>
        </div>
    `).join('');
}

// Health check
async function checkHealth() {
    try {
        const response = await fetch(`${API_BASE}/health`);
        if (!response.ok) return;

        const health = await response.json();
        updateHealthStatus(health);
    } catch (error) {
        console.error('Erreur lors de la vérification de santé', error);
        updateHealthStatus({ status: 'DOWN', naps2Available: false, databaseConnected: false });
    }
}

function updateHealthStatus(health) {
    const healthStatus = document.getElementById('healthStatus');
    const naps2Status = document.getElementById('naps2Status');
    const dbStatus = document.getElementById('dbStatus');
    const globalStatus = document.getElementById('globalStatus');

    healthStatus.textContent = health.status === 'UP' ? 'Opérationnel' : 
                              health.status === 'DEGRADED' ? 'Dégradé' : 'Indisponible';
    healthStatus.className = 'status-badge ' + health.status.toLowerCase();

    naps2Status.textContent = health.naps2Available ? 'Disponible' : 'Indisponible';
    naps2Status.className = 'status-value ' + (health.naps2Available ? 'available' : 'unavailable');

    dbStatus.textContent = health.databaseConnected ? 'Connectée' : 'Déconnectée';
    dbStatus.className = 'status-value ' + (health.databaseConnected ? 'available' : 'unavailable');

    globalStatus.textContent = health.status === 'UP' ? 'Opérationnel' : 
                              health.status === 'DEGRADED' ? 'Dégradé' : 'Indisponible';
    globalStatus.className = 'status-value ' + (health.status === 'UP' ? 'available' : 'unavailable');
}

function showMessage(type, message) {
    const container = document.querySelector('.container');
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${type}`;
    messageDiv.textContent = message;
    
    container.insertBefore(messageDiv, container.firstChild);
    
    setTimeout(() => {
        messageDiv.remove();
    }, 5000);
}

