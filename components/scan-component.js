/**
 * Web Component Modal pour le scan de documents
 */
class ScanModal extends HTMLElement {
    constructor() {
        super();
        this.attachShadow({ mode: 'open' });
        this.api = null;
        this.scanId = null;
        this.statusInterval = null;
        this.pdfBlob = null;
        this.pdfFile = null;
    }

    static get observedAttributes() {
        return ['api-base-url', 'target-input-id'];
    }

    connectedCallback() {
        this.render();
        this.setupEventListeners();
        const apiUrl = this.getAttribute('api-base-url') || 'http://localhost:7070/api/scans';
        this.api = new ScanAPI(apiUrl);
    }

    disconnectedCallback() {
        this.stopStatusPolling();
    }

    attributeChangedCallback(name, oldValue, newValue) {
        if (name === 'api-base-url' && this.api) {
            this.api.baseUrl = newValue || 'http://localhost:7070/api/scans';
        }
    }

    render() {
        // Charger les styles depuis le fichier CSS
        const styles = this.getStyles();

        const template = `
            <div class="modal-overlay" id="overlay">
                <div class="modal-container">
                    <div class="modal-header">
                        <h2 class="modal-title">Scanner un document</h2>
                        <button class="modal-close" id="closeBtn" aria-label="Fermer">×</button>
                    </div>
                    <div class="modal-body">
                        <div class="preview-panel" id="previewPanel">
                            <div class="preview-placeholder">
                                <p>Aperçu du document scanné</p>
                                <p style="font-size: 14px; color: #999; margin-top: 10px;">Le PDF apparaîtra ici après le scan</p>
                            </div>
                        </div>
                        <div class="config-panel">
                            <div class="config-section">
                                <h3 class="config-title">Configuration du scan</h3>
                                <p class="config-message">Configurez les paramètres de scan ci-dessous, puis cliquez sur "Scanner" pour lancer le scan.</p>
                            </div>
                            
                            <div class="config-group">
                                <label class="config-label" for="scannerName">Scanner</label>
                                <select class="config-select" id="scannerName">
                                    <option value="">Chargement des scanners...</option>
                                </select>
                            </div>

                            <div class="config-group">
                                <label class="config-label" for="pageSize">Taille de papier</label>
                                <select class="config-select" id="pageSize">
                                    <option value="A4">A4</option>
                                    <option value="A3">A3</option>
                                    <option value="Letter">Letter</option>
                                    <option value="Legal">Legal</option>
                                    <option value="A5">A5</option>
                                    <option value="B4">B4</option>
                                    <option value="B5">B5</option>
                                    <option value="Executive">Executive</option>
                                    <option value="Folio">Folio</option>
                                    <option value="Tabloid">Tabloid</option>
                                </select>
                            </div>

                            <div class="config-group">
                                <label class="config-label" for="colorMode">Mode couleur</label>
                                <select class="config-select" id="colorMode">
                                    <option value="Color">Couleur</option>
                                    <option value="Grayscale">Niveaux de gris</option>
                                    <option value="BlackAndWhite">Noir et blanc</option>
                                </select>
                            </div>

                            <div class="config-group">
                                <label class="config-label" for="driver">Driver</label>
                                <select class="config-select" id="driver">
                                    <option value="wia">WIA (Windows)</option>
                                    <option value="twain">TWAIN</option>
                                    <option value="sane">SANE (Linux)</option>
                                </select>
                            </div>

                            <div class="config-group">
                                <label class="config-label" for="source">Source</label>
                                <select class="config-select" id="source">
                                    <option value="glass">Vitre (glass)</option>
                                    <option value="feeder">Chargeur (feeder)</option>
                                    <option value="duplex">Recto-verso (duplex)</option>
                                </select>
                            </div>

                            <div class="config-group">
                                <label class="config-label" for="resolution">Résolution (DPI)</label>
                                <input type="number" class="config-input" id="resolution" value="300" min="75" max="1200" step="25">
                            </div>

                            <div class="config-buttons">
                                <button class="config-button button-cancel" id="cancelBtn">Annuler</button>
                                <button class="config-button button-scan" id="scanBtn">Scanner</button>
                                <button class="config-button button-validate" id="validateBtn" disabled>Valider</button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;

        this.shadowRoot.innerHTML = `<style>${styles}</style>${template}`;
    }

    getStyles() {
        // Styles intégrés pour le Shadow DOM
        return `
            :host {
                display: none;
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                z-index: 10000;
            }

            :host([open]) {
                display: block;
            }

            .modal-overlay {
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background-color: rgba(0, 0, 0, 0.5);
                backdrop-filter: blur(4px);
                display: flex;
                align-items: center;
                justify-content: center;
                animation: fadeIn 0.2s ease-in-out;
            }

            .modal-container {
                background: white;
                border-radius: 8px;
                box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
                width: 90%;
                max-width: 1200px;
                max-height: 90vh;
                display: flex;
                flex-direction: column;
                animation: slideIn 0.3s ease-out;
                overflow: hidden;
            }

            .modal-header {
                padding: 20px;
                border-bottom: 1px solid #e0e0e0;
                display: flex;
                justify-content: space-between;
                align-items: center;
                background: #f8f9fa;
            }

            .modal-title {
                margin: 0;
                font-size: 24px;
                font-weight: 600;
                color: #333;
            }

            .modal-close {
                background: none;
                border: none;
                font-size: 28px;
                cursor: pointer;
                color: #666;
                padding: 0;
                width: 32px;
                height: 32px;
                display: flex;
                align-items: center;
                justify-content: center;
                border-radius: 4px;
                transition: all 0.2s;
            }

            .modal-close:hover {
                background: #e0e0e0;
                color: #333;
            }

            .modal-body {
                display: grid;
                grid-template-columns: 2fr 1fr;
                gap: 0;
                flex: 1;
                overflow: hidden;
                min-height: 0;
            }

            .preview-panel {
                padding: 20px;
                background: #f5f5f5;
                overflow-y: auto;
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                min-height: 100%;
            }

            .preview-placeholder {
                text-align: center;
                color: #999;
                font-size: 16px;
            }

            .preview-pdf {
                width: 100%;
                max-width: 100%;
                height: 90%;
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
                border-radius: 4px;
            }

            .preview-loading {
                display: flex;
                flex-direction: column;
                align-items: center;
                gap: 20px;
            }

            .spinner {
                width: 40px;
                height: 40px;
                border: 4px solid #f3f3f3;
                border-top: 4px solid #007bff;
                border-radius: 50%;
                animation: spin 1s linear infinite;
            }

            .preview-status {
                color: #666;
                font-size: 14px;
            }

            .preview-error {
                color: #dc3545;
                text-align: center;
                padding: 20px;
            }

            .config-panel {
                padding: 20px;
                background: white;
                overflow-y: auto;
                border-left: 1px solid #e0e0e0;
            }

            .config-section {
                margin-bottom: 24px;
            }

            .config-title {
                font-size: 18px;
                font-weight: 600;
                margin-bottom: 8px;
                color: #333;
            }

            .config-message {
                font-size: 14px;
                color: #666;
                margin-bottom: 20px;
                line-height: 1.5;
            }

            .config-group {
                margin-bottom: 16px;
            }

            .config-label {
                display: block;
                margin-bottom: 6px;
                font-weight: 500;
                color: #555;
                font-size: 14px;
            }

            .config-input,
            .config-select {
                width: 100%;
                padding: 8px 12px;
                border: 1px solid #ddd;
                border-radius: 4px;
                font-size: 14px;
                transition: border-color 0.2s;
                box-sizing: border-box;
            }

            .config-input:focus,
            .config-select:focus {
                outline: none;
                border-color: #007bff;
                box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
            }

            .config-buttons {
                display: flex;
                gap: 10px;
                margin-top: 24px;
                flex-wrap: wrap;
            }

            .config-button {
                flex: 1;
                min-width: 100px;
                padding: 12px 20px;
                border: none;
                border-radius: 4px;
                font-size: 14px;
                font-weight: 500;
                cursor: pointer;
                transition: all 0.2s;
            }

            .config-button:disabled {
                opacity: 0.5;
                cursor: not-allowed;
            }

            .button-cancel {
                background: #6c757d;
                color: white;
            }

            .button-cancel:hover:not(:disabled) {
                background: #5a6268;
            }

            .button-scan {
                background: #007bff;
                color: white;
            }

            .button-scan:hover:not(:disabled) {
                background: #0056b3;
            }

            .button-validate {
                background: #28a745;
                color: white;
            }

            .button-validate:hover:not(:disabled) {
                background: #218838;
            }

            @keyframes fadeIn {
                from { opacity: 0; }
                to { opacity: 1; }
            }

            @keyframes slideIn {
                from {
                    transform: translateY(-20px);
                    opacity: 0;
                }
                to {
                    transform: translateY(0);
                    opacity: 1;
                }
            }

            @keyframes spin {
                0% { transform: rotate(0deg); }
                100% { transform: rotate(360deg); }
            }

            @media (max-width: 768px) {
                .modal-body {
                    grid-template-columns: 1fr;
                    grid-template-rows: 1fr auto;
                }

                .config-panel {
                    border-left: none;
                    border-top: 1px solid #e0e0e0;
                    max-height: 50vh;
                }
            }
        `;
    }

    setupEventListeners() {
        const overlay = this.shadowRoot.getElementById('overlay');
        const closeBtn = this.shadowRoot.getElementById('closeBtn');
        const cancelBtn = this.shadowRoot.getElementById('cancelBtn');
        const scanBtn = this.shadowRoot.getElementById('scanBtn');
        const validateBtn = this.shadowRoot.getElementById('validateBtn');
        const driverSelect = this.shadowRoot.getElementById('driver');

        // Fermeture
        closeBtn.addEventListener('click', () => this.close());
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) {
                this.close();
            }
        });

        // Touche Escape
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.hasAttribute('open')) {
                this.close();
            }
        });

        // Boutons
        cancelBtn.addEventListener('click', () => this.handleCancel());
        scanBtn.addEventListener('click', () => this.handleScan());
        validateBtn.addEventListener('click', () => this.handleValidate());

        // Synchronisation driver ↔ devices
        if (driverSelect) {
            driverSelect.addEventListener('change', () => {
                const newDriver = driverSelect.value;
                // Réinitialiser la sélection du scanner
                const scannerSelect = this.shadowRoot.getElementById('scannerName');
                if (scannerSelect) {
                    scannerSelect.value = '';
                }
                // Recharger les devices pour le nouveau driver
                this.loadDevices(newDriver);
            });
        }
    }

    open() {
        this.setAttribute('open', '');
        
        // Charger les devices avec le driver actuellement sélectionné
        const driverSelect = this.shadowRoot.getElementById('driver');
        const currentDriver = driverSelect ? driverSelect.value : null;
        this.loadDevices(currentDriver);
        
        // Focus trap
        const firstInput = this.shadowRoot.querySelector('.config-input, .config-select');
        if (firstInput) {
            setTimeout(() => firstInput.focus(), 100);
        }
    }

    close() {
        this.removeAttribute('open');
        this.handleCancel();
        this.dispatchEvent(new CustomEvent('scan-cancelled'));
    }

    async handleCancel() {
        if (this.scanId) {
            try {
                await this.api.cancelScan(this.scanId);
            } catch (error) {
                console.warn('Erreur lors de l\'annulation:', error);
            }
        }
        this.reset();
    }

    reset() {
        this.stopStatusPolling();
        this.scanId = null;
        this.pdfBlob = null;
        this.pdfFile = null;
        
        const previewPanel = this.shadowRoot.getElementById('previewPanel');
        previewPanel.innerHTML = `
            <div class="preview-placeholder">
                <p>Aperçu du document scanné</p>
                <p style="font-size: 14px; color: #999; margin-top: 10px;">Le PDF apparaîtra ici après le scan</p>
            </div>
        `;

        const validateBtn = this.shadowRoot.getElementById('validateBtn');
        validateBtn.disabled = true;

        const scanBtn = this.shadowRoot.getElementById('scanBtn');
        scanBtn.disabled = false;
        scanBtn.textContent = 'Scanner';
    }

    async handleScan() {
        const scanBtn = this.shadowRoot.getElementById('scanBtn');
        scanBtn.disabled = true;
        scanBtn.textContent = 'Scan en cours...';

        try {
            // Récupérer la configuration
            const config = this.getConfig();

            // Créer la session de scan
            const createResponse = await this.api.createScan(config);
            this.scanId = createResponse.scanId;

            // Démarrer le scan
            await this.api.startScan(this.scanId);

            // Démarrer le polling du statut
            this.startStatusPolling();

        } catch (error) {
            this.showError(error.message);
            scanBtn.disabled = false;
            scanBtn.textContent = 'Scanner';
        }
    }

    async loadDevices(driver = null) {
        const scannerSelect = this.shadowRoot.getElementById('scannerName');
        const scanBtn = this.shadowRoot.getElementById('scanBtn');
        
        // Afficher l'état de chargement
        scannerSelect.innerHTML = '<option value="">Chargement des scanners...</option>';
        scannerSelect.disabled = true;
        
        try {
            const response = await this.api.listDevices(driver);
            const devices = response.devices || [];
            
            // Vider le select
            scannerSelect.innerHTML = '';
            
            if (devices.length === 0) {
                // Aucun scanner disponible
                scannerSelect.innerHTML = '<option value="">Aucun scanner disponible</option>';
                scanBtn.disabled = true;
            } else {
                // Ajouter une option par défaut
                scannerSelect.innerHTML = '<option value="">-- Sélectionner un scanner --</option>';
                
                // Ajouter chaque scanner
                devices.forEach(device => {
                    const option = document.createElement('option');
                    option.value = device.name;
                    option.textContent = device.name;
                    scannerSelect.appendChild(option);
                });
                
                // Si un seul scanner, le sélectionner automatiquement
                if (devices.length === 1) {
                    scannerSelect.value = devices[0].name;
                }
                
                scanBtn.disabled = false;
            }
            
            scannerSelect.disabled = false;
            
        } catch (error) {
            console.error('Erreur lors du chargement des scanners:', error);
            
            // Afficher le message d'erreur
            scannerSelect.innerHTML = `<option value="">${error.message || 'Erreur de chargement des scanners'}</option>`;
            scannerSelect.disabled = false;
            scanBtn.disabled = true;
        }
    }

    getConfig() {
        const scannerName = this.shadowRoot.getElementById('scannerName').value;
        const pageSize = this.shadowRoot.getElementById('pageSize').value;
        const colorMode = this.shadowRoot.getElementById('colorMode').value;
        const driver = this.shadowRoot.getElementById('driver').value;
        const source = this.shadowRoot.getElementById('source').value;
        const resolution = parseInt(this.shadowRoot.getElementById('resolution').value) || 300;

        return {
            scannerName: scannerName || null,
            options: {
                pageSize: pageSize,
                colorMode: colorMode,
                driver: driver,
                source: source,
                resolution: resolution
            }
        };
    }

    startStatusPolling() {
        this.stopStatusPolling();
        this.statusInterval = setInterval(async () => {
            try {
                const status = await this.api.getScanStatus(this.scanId);
                this.updateStatus(status);

                if (status.status === 'COMPLETED') {
                    this.stopStatusPolling();
                    await this.loadPdf();
                } else if (status.status === 'FAILED' || status.status === 'CANCELLED') {
                    this.stopStatusPolling();
                    this.showError(status.errorMessage || 'Le scan a échoué');
                }
            } catch (error) {
                console.error('Erreur lors de la vérification du statut:', error);
                this.stopStatusPolling();
                this.showError(error.message);
            }
        }, 1000);
    }

    stopStatusPolling() {
        if (this.statusInterval) {
            clearInterval(this.statusInterval);
            this.statusInterval = null;
        }
    }

    updateStatus(status) {
        const previewPanel = this.shadowRoot.getElementById('previewPanel');
        const scanBtn = this.shadowRoot.getElementById('scanBtn');

        let statusText = '';
        switch (status.status) {
            case 'PENDING':
                statusText = 'En attente...';
                break;
            case 'RUNNING':
                statusText = 'Scan en cours...';
                break;
            case 'COMPLETED':
                statusText = 'Scan terminé';
                break;
            case 'FAILED':
                statusText = 'Échec du scan';
                break;
            case 'CANCELLED':
                statusText = 'Scan annulé';
                break;
        }

        if (status.status === 'RUNNING' || status.status === 'PENDING') {
            previewPanel.innerHTML = `
                <div class="preview-loading">
                    <div class="spinner"></div>
                    <p class="preview-status">${statusText}</p>
                </div>
            `;
        }
    }

    async loadPdf() {
        try {
            const previewPanel = this.shadowRoot.getElementById('previewPanel');
            previewPanel.innerHTML = `
                <div class="preview-loading">
                    <div class="spinner"></div>
                    <p class="preview-status">Chargement du PDF...</p>
                </div>
            `;

            // Télécharger le PDF
            this.pdfBlob = await this.api.getPdf(this.scanId);
            
            // Créer un File object pour l'injection dans l'input
            const fileName = `scan_${this.scanId}.pdf`;
            this.pdfFile = new File([this.pdfBlob], fileName, { type: 'application/pdf' });

            // Afficher le PDF
            const pdfUrl = URL.createObjectURL(this.pdfBlob);
            previewPanel.innerHTML = `
                <iframe src="${pdfUrl}" class="preview-pdf" frameborder="0"></iframe>
            `;

            // Activer le bouton Valider
            const validateBtn = this.shadowRoot.getElementById('validateBtn');
            validateBtn.disabled = false;

            const scanBtn = this.shadowRoot.getElementById('scanBtn');
            scanBtn.disabled = false;
            scanBtn.textContent = 'Scanner';

        } catch (error) {
            this.showError(error.message);
        }
    }

    showError(message) {
        const previewPanel = this.shadowRoot.getElementById('previewPanel');
        previewPanel.innerHTML = `
            <div class="preview-error">
                <p><strong>Erreur:</strong> ${message}</p>
            </div>
        `;

        const scanBtn = this.shadowRoot.getElementById('scanBtn');
        scanBtn.disabled = false;
        scanBtn.textContent = 'Scanner';
    }

    handleValidate() {
        if (!this.pdfFile) {
            return;
        }

        const targetInputId = this.getAttribute('target-input-id');
        
        if (targetInputId) {
            // Injection dans l'input file cible
            const targetInput = document.getElementById(targetInputId);
            if (targetInput) {
                const dataTransfer = new DataTransfer();
                dataTransfer.items.add(this.pdfFile);
                targetInput.files = dataTransfer.files;
                
                // Déclencher l'événement change
                targetInput.dispatchEvent(new Event('change', { bubbles: true }));
            }
        }

        // Émettre l'événement scan-completed
        this.dispatchEvent(new CustomEvent('scan-completed', {
            detail: { file: this.pdfFile }
        }));

        // Fermer la modal
        this.close();
    }
}

// Enregistrer le web component
customElements.define('scan-modal', ScanModal);

