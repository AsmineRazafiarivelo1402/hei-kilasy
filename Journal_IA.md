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

_____________________________________________________________________________
TD Kilasy : Notification par SMS (C2)

### Échange 1 — 21:37 à 21:41
Demande : choisir une solution pour envoyer un SMS lorsqu’un enseignant publie une note.

Décision : le fournisseur SMS n’étant pas choisi, l’envoi sera simulé par un message écrit dans les logs. Il est déclenché lors de la publication de la note ; aucun SMS réel n’est envoyé.

### Échange 2 — 22:15 à 22:19
Proposition de l’utilisateur : créer une nouvelle classe contenant les informations du SMS (numéro destinataire et contenu) et prendre en compte les échecs possibles : numéro invalide ou absent et panne du service d’envoi. Une panne ne doit pas empêcher la publication de la note.

Décision : séparer le service d’envoi de la notation afin de pouvoir remplacer le fournisseur simulé plus tard sans modifier le code de notation. L’utilisateur a demandé la mise en œuvre de cette conception.

### Échange 3 — Implémentation et vérification
Implémentation : ajouter `SmsMessage`, `SmsSender`, `SimulatedSmsSender` et `SmsNotificationService`. Appeler ce dernier après l’enregistrement de la note et conserver l’envoi d’email existant. Les numéros absents ou invalides et les exceptions du composant SMS sont journalisés sans annuler la publication de la note.

Vérification : tests unitaires du format du message, des numéros absents ou invalides, et d’une panne simulée. Le test du contrôleur vérifie que l’UPDATE et la réponse contenant la note finale réussissent quand le composant SMS lève une exception. `mvn test` réussit : 4 tests, 0 échec.

## Bilan

Fichiers modifiés pour la fonctionnalité :

- `src/main/java/hei/school/SmsMessage.java` — contient le numéro destinataire et le texte du SMS.
- `src/main/java/hei/school/SmsSender.java` — définit le contrat du composant d’envoi remplaçable.
- `src/main/java/hei/school/SimulatedSmsSender.java` — simule l’envoi en écrivant le message dans les logs.
- `src/main/java/hei/school/SmsNotificationService.java` — prépare le message, vérifie le numéro et isole les erreurs d’envoi.
- `src/main/java/hei/school/SubmissionController.java` — déclenche la notification SMS après l’enregistrement de la note.
- `src/test/java/hei/school/SmsNotificationServiceTest.java` — teste le message, les numéros invalides/absents et la panne simulée.
- `src/test/java/hei/school/SubmissionControllerSmsFailureTest.java` — vérifie que la note reste publiée en cas de panne SMS.
- `pom.xml` — ajoute les dépendances nécessaires aux tests.
- `README.md` — documente la simulation SMS.


_____________________________________________________________________________
TD Kilasy : Exercices minutés (C3)

### Échange 1 — Conception de la durée


Décisions précisées par l'utilisateur :


### Échange 1 — Schéma et création d'exercice ( 11h )
Implémentation : ajouter `duration_minutes` nullable avec une contrainte de valeur positive à la table `exercise`, rendre `deadline` nullable et créer `exercise_start` pour enregistrer le début par couple exercice/étudiant. La durée maximale appartient à l'exercice ; `started_at` appartient au démarrage individuel.

L'endpoint `POST /exercises` accepte une durée entière positive. Il exige au moins une échéance ou une durée, et renvoie la durée dans sa réponse. Les lectures des exercices acceptent une échéance absente.

Erreur de direction corrigée : une première proposition transformait le schéma historique et les données de démonstration en migrations Flyway. L'utilisateur l'a refusée ; Flyway a été retiré et les modifications de schéma ont été placées dans `schema.sql`, conformément à la consigne.

### Échange 2 — Démarrage et validation des soumissions
Implémentation : `POST /exercises/{id}/students/{studentId}/start` vérifie l'existence de l'exercice et de l'étudiant puis enregistre `started_at` une seule fois. Un nouvel appel ne remet pas le chronomètre à zéro. Pour un exercice sans durée, cet endpoint répond qu'aucun minuteur n'est nécessaire ; la soumission reste possible.

Pour un exercice minuté, la soumission vérifie qu'un démarrage existe, puis compare l'heure de soumission à `started_at + duration_minutes`. Une soumission après l'expiration est refusée en HTTP 422 avec un message explicite ; l'insertion n'est pas exécutée. Le retard par rapport à une échéance éventuelle est évalué séparément, afin de conserver la pénalité C1.



### Bilan
Fichiers concernés par C3 :

- `src/main/resources/schema.sql` — échéance facultative, durée en minutes et table des démarrages.
- `src/main/resources/data.sql` — exercice minuté de démonstration.
- `src/main/java/hei/school/ExerciseController.java` — création d'exercices avec durée et échéance facultative.
- `src/main/java/hei/school/SubmissionController.java` — démarrage, contrôle du minuteur, refus HTTP 422 et retard lié à l'échéance.
- `src/main/java/hei/school/GradeReport.java` — temps utilisé dans le relevé.
- `src/main/java/hei/school/GradeCalculator.java` — absence d'échéance traitée sans pénalité.
- `src/main/java/hei/school/CsvExport.java` — absence d'échéance traitée dans l'export.
- `src/test/java/hei/school/SubmissionDurationTest.java` — tests du démarrage et du dépassement de durée.
- `src/test/java/hei/school/GradeCalculatorTest.java` — tests de la pénalité de retard.
- `README.md` — consignes de recréation de la base et exemples pour tester l'exercice minuté.


