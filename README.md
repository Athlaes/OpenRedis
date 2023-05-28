# OpenRedis

Bienvenue sur l'application client / serveur OpenRedis. Cette application est réalisé dans le cadre d'un projet à l'université de lorraine. 

# Installation
Pour ce qui est de l'installation rien de plus simple, deux jars vous sont fournis, avec 2 fichiers bat pour lancer les .jars.

  * Sur windows : double cliquer sur les fichiers .bat fournis pour ouvrir l'application associée.
  * Sur les autres environnements : ouvrez un terminal à l'endroit ou vous avez décidé de stocker les .jar et tapez la commande suivante : ```java -jar <nomdujar>.jar```

# Utilisation du client

Lorsque vous avez lancé le client vous pouvez saisir le port du serveur sur lequel les requêtes seront envoyés. Vous ne pourrez plus changer le port durant l'exécution du client.

La commande ```quit``` permet de quitter le client.

# Utilisation du serveur

Lorsque le serveur se lance, vous serez invité à chosir si vous souhaitez ouvrir un serveur master ou un serveur esclave ainsi qu'à saisir un port d'écoute, cette fonctionnalité permet de changer le port d'écoute du slave.

## Commandes prisent en charge

  Le serveur prend en charge les commandes suivantes : 

  * ```set key value```
  * ```setnx key value```
  * ```get key```
  * ```STRLEN key```
  * ```APPEND key « chaîne à concaténer »```
  * ```INCR key```
  * ```DECR key```
  * ```EXISTS key```
  * ```DEL key```
  * ```EXPIRE key duration```
  * ```subscribe channel```
  * ```unsubscribe``` - en souscription
  * ```publish channel message```

  Le pipelining et le multiligne "\n" sont fonctionnels. Opérateur de pipelining : >.

  Exemple : 
  ```set test 1 > get test > set test 2 > get 2```
  ``` "OK" ``` 
  ``` (integer) 1 ```
  ``` "OK" ``` 
  ``` (integer) 2 ```

## Utilisation du mode master / slave

  Vous pouvez connecter différents slaves à un serveur master en tapant certaines commandes sur le master. 
  Lorsque le serveur slave se connecte il synchronise les données du master puis gère les requêtes lorsqu'elles arrivent comme le master. Seules les requêtes d'écriture sont envoyées aux esclaves.

### Sur master 

  La commande ```connect <port>``` permet de connecter un serveur lancé sur la même machine avec le port <port\>.

### Sur slave 

  Le slave peut devenir master avec la commande ```make master```. Lorsqu'il devient master il écoute les requêtes des sockets sur son port et remplie son rôle comme le ferait un serveur démarré en mode master.