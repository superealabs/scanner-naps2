    /**
     * Client API pour communiquer avec l'API scanner
     */
    class ScanAPI {
        constructor(baseUrl = 'http://localhost:8090/api/scans') {
            this.baseUrl = baseUrl;
        }

        /**
         * Créer une nouvelle session de scan
         * @param {Object} config - Configuration du scan
         * @param {string} config.scannerName - Nom du scanner
         * @param {Object} config.options - Options de scan (pageSize, colorMode, resolution, driver, source)
         * @returns {Promise<Object>} Réponse avec scanId
         */
        async createScan(config) {
            const response = await fetch(this.baseUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(config)
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || `Erreur HTTP: ${response.status}`);
            }

            return await response.json();
        }

        /**
         * Démarrer un scan
         * @param {string} scanId - ID de la session de scan
         * @returns {Promise<Object>} Réponse avec le statut
         */
        async startScan(scanId) {
            const response = await fetch(`${this.baseUrl}/${scanId}/start`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || `Erreur HTTP: ${response.status}`);
            }

            return await response.json();
        }

        /**
         * Récupérer le statut d'un scan
         * @param {string} scanId - ID de la session de scan
         * @returns {Promise<Object>} Réponse avec le statut
         */
        async getScanStatus(scanId) {
            const response = await fetch(`${this.baseUrl}/${scanId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || `Erreur HTTP: ${response.status}`);
            }

            return await response.json();
        }

        /**
         * Télécharger le PDF d'un scan
         * @param {string} scanId - ID de la session de scan
         * @returns {Promise<Blob>} Blob du PDF
         */
        async getPdf(scanId) {
            const response = await fetch(`${this.baseUrl}/${scanId}/pdf`, {
                method: 'GET'
            });

            if (!response.ok) {
                const error = await response.json().catch(() => ({ message: `Erreur HTTP: ${response.status}` }));
                throw new Error(error.message || `Erreur HTTP: ${response.status}`);
            }

            return await response.blob();
        }

        /**
         * Annuler un scan
         * @param {string} scanId - ID de la session de scan
         * @returns {Promise<Object>} Réponse avec le statut
         */
        async cancelScan(scanId) {
            const response = await fetch(`${this.baseUrl}/${scanId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || `Erreur HTTP: ${response.status}`);
            }

            return await response.json();
        }

        /**
         * Lister les périphériques de scan disponibles
         * @param {string} driver - Driver optionnel pour filtrer (wia, twain, escl, sane, apple)
         * @returns {Promise<Object>} Réponse avec la liste des périphériques { devices: [...], count: N }
         */
        async listDevices(driver = null) {
            // Construire l'URL en remplaçant /scans par /devices
            const devicesUrl = this.baseUrl.replace("/scans","/devices");
            
            // Ajouter le paramètre driver si fourni
            const url = driver ? `${devicesUrl}?driver=${encodeURIComponent(driver)}` : devicesUrl;
            
            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                const error = await response.json().catch(() => ({ 
                    message: `Erreur HTTP: ${response.status}` 
                }));
                
                // Messages d'erreur spécifiques selon le code de statut
                if (response.status === 503) {
                    throw new Error('NAPS2 non disponible');
                } else if (response.status === 500) {
                    throw new Error(error.message || 'Erreur serveur lors du chargement des scanners');
                } else {
                    throw new Error(error.message || `Erreur HTTP: ${response.status}`);
                }
            }

            return await response.json();
        }
    }

