# Journal IA — Kilasy

Outil utilisé : Copilot dans IntelliJ IDEA

## Changement : application d’une pénalité de retard sur les notes (C1)

### Échange 1 — 20:31
Demande : comprendre la méthode `PUT /submissions/{id}/grade` dans `SubmissionController`, en particulier le fonctionnement de la notation.

Explication : `raw_points` contient les points attribués par l’enseignant sur le barème de l’exercice (`max_points`). La note sur 20 est calculée à partir de ces deux valeurs. La méthode vérifie la soumission et le barème, enregistre les points bruts et le commentaire, puis envoie la note par email.

### Échange 2 — 20:49 à 21:48
Discussion : déterminer comment appliquer la pénalité sans perdre la note donnée par l’enseignant.

Décision : garder `raw_points` comme note brute en base et calculer la pénalité séparément. La note finale est calculée à partir de la note brute sur 20, du pourcentage de pénalité et des points retirés :

```text
penaltyPoints = grade × penaltyPercent / 100
finalGrade = grade − penaltyPoints
```

Le pourcentage augmente de 10 % par jour de retard entamé et est plafonné à 50 %. Une soumission à l’heure n’est pas pénalisée.

### Échange 3 — 21:15 à 21:53
Demande client : appliquer une pénalité de 10 % par jour de retard entamé, avec un plafond de 50 %. La même note pénalisée doit apparaître dans la correction, le relevé de notes et l’export CSV ; une note ne doit jamais être négative.

Décision sur l’échéance : remplacer le type `DATE` par `TIMESTAMP`. Les échéances déjà en base sont interprétées à 23:59:59 le jour enregistré. Une date seule fournie à la création d’un exercice reçoit aussi cette heure par défaut. La pénalité est calculée selon les tranches de 24 heures entamées après l’échéance.

### Échange 4 — 21:53 à 22:12
Implémentation : ajouter un calculateur commun pour produire la note brute sur 20, le pourcentage de pénalité, les points retirés et la note finale. Utiliser le calcul dans la correction, la consultation de soumission, le relevé et le CSV.

Clarification : le bloc `BigDecimal` ajouté temporairement pour valider la saisie des points a été remplacé par la validation antérieure en `double`, à la demande de l’utilisateur. Le calcul partagé de la pénalité est conservé.

### Échange 5 — 22:14
Vérification : l’application a démarré avec la base PostgreSQL et le serveur mail disponibles. Pour une note brute de 16/20 rendue deux heures après l’échéance, l’API de consultation, le relevé et le CSV affichent 14,4/20 avec une pénalité de 10 % (1,6 point retiré). `mvn test` réussit.

## Bilan

Fichiers modifiés pour la fonctionnalité :

- `src/main/java/hei/school/GradeCalculator.java` — calcul commun de la pénalité et de la note finale.
- `src/main/java/hei/school/DeadlineMigration.java` — conversion des anciennes échéances `DATE` en `TIMESTAMP` à 23:59:59.
- `src/main/resources/schema.sql` — nouveau type `TIMESTAMP` pour `exercise.deadline`.
- `src/main/resources/data.sql` — échéances des exercices de démonstration à 23:59:59.
- `src/main/java/hei/school/Exercise.java` — échéance représentée par un `LocalDateTime`.
- `src/main/java/hei/school/ExerciseController.java` — lecture et création des échéances avec heure, avec 23:59:59 par défaut pour une date seule.
- `src/main/java/hei/school/SubmissionController.java` — calcul et retour de la note finale pénalisée pendant la correction et la consultation.
- `src/main/java/hei/school/GradeReport.java` — note pénalisée utilisée dans le relevé et dans la moyenne.
- `src/main/java/hei/school/CsvExport.java` — note pénalisée et statut de retard dans l’export.

`src/main/java/hei/school/SubmissionController.java` contenait déjà des modifications locales avant cette intervention ; elles ont été conservées.

Validation : `mvn test`, vérification des exemples de pénalité et essai des réponses de consultation, du relevé et du CSV.
