# Solution à l'erreur SQLIntegrityConstraintViolationException

## Problème
L'erreur suivante s'est produite :
```
java.sql.SQLIntegrityConstraintViolationException: Cannot add or update a child row: 
a foreign key constraint fails (`fintech`.`formations`, CONSTRAINT `fk_userIdFormation` 
FOREIGN KEY (`user_id`) REFERENCES `utilisateur` (`id_user`))
```

## Cause
La table `formations` a une clé étrangère `user_id` qui référence la table `utilisateur`. 
L'ID utilisateur utilisé (1) n'existe pas dans la table `utilisateur`.

## Solution

### Étape 1 : Vérifier les utilisateurs existants
Exécutez cette requête dans votre base de données :
```sql
SELECT id_user, nom, prenom FROM utilisateur;
```

### Étape 2 : Utiliser un ID utilisateur existant
Si des utilisateurs existent, notez leur ID et modifiez le fichier `Test.java` :
```java
// Remplacez 2 par un ID d'utilisateur existant
```

### Étape 3 : Ou créer un nouvel utilisateur
Si aucun utilisateur n'existe, créez-en un :
```sql
INSERT INTO utilisateur (nom, prenom, mot_de_passe, telephone, piece_identite, user_image, role)
VALUES ('Admin', 'System', 'password123', '21234567', 'CIN000000', 'admin.jpg', 'FORMATEUR');
```

Puis récupérez l'ID généré :
```sql
SELECT LAST_INSERT_ID();
```

## Modifications apportées
1. ✅ Ajout du champ `user_id` au modèle `formations.java`
2. ✅ Ajout des getter/setter pour `user_id`
3. ✅ Modification de `FormationService.insertOne()` pour utiliser le `user_id` du modèle
4. ✅ Modification de `FormationService.SelectAll()` pour récupérer le `user_id`
5. ✅ Modification du `Test.java` pour définir le `user_id` avant l'insertion

## Prochaines étapes
1. Vérifiez les utilisateurs existants dans votre base de données
2. Modifiez le `user_id` dans `Test.java` avec un ID existant
3. Exécutez le test à nouveau

