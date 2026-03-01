-- Insérer un utilisateur de test avec l'ID 1
-- Assurez-vous que l'ID 1 n'existe pas déjà avant d'exécuter cela
INSERT INTO utilisateur (id_user, nom, prenom, mot_de_passe, telephone, piece_identite, user_image, role)
VALUES (1, 'Admin', 'System', 'password123', '21234567', 'CIN000000', 'admin.jpg', 'FORMATEUR');

-- Ou si vous préférez laisser MySQL générer l'ID automatiquement :
-- INSERT INTO utilisateur (nom, prenom, mot_de_passe, telephone, piece_identite, user_image, role)
-- VALUES ('Admin', 'System', 'password123', '21234567', 'CIN000000', 'admin.jpg', 'FORMATEUR');

-- Ensuite, utilisez SELECT pour récupérer l'ID généré :
-- SELECT LAST_INSERT_ID();

