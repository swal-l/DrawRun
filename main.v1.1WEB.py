# -*- coding: utf-8 -*-

VolumeTps = float(input("Entrez le volume de temps total hebdomadaire (en heures) de course à pied : "))
NBsemaineProg = int(input("Entrez le nombre de semaine pour votre entrainement :"))
objectif = int(input("Entrez votre objectif en km :"))

Kbase = 10
K = Kbase + (objectif * 0.5)
Sd = objectif / (21 + K)
phaseGenerale = round(((40 + (30 * Sd))/100) * NBsemaineProg)
phaseAffutage = round(0.1 * NBsemaineProg)
phaseSpecifique = NBsemaineProg - phaseGenerale - phaseAffutage

i = 0
n = 0  # Compteur pour alterner les séances VMA
m = 0  # Compteur pour alterner les séances Seuil


def TPSSeancesSemaine(VolumeTpsH):
    """Calcule la répartition du temps d'entraînement pour la semaine."""
    VolumeTpsM = int(VolumeTpsH * 60)  # Volume total en minutes
    
    # Calcule le nombre de séances (exemple : 3h -> 6 séances, 5h -> 8 séances)
    nbSeances = int(VolumeTpsH + 3) 

    # Répartition 80% Endurance Fondamentale (EF) / 20% Qualité (Q)
    VolumeEF = int(0.8 * VolumeTpsM)
    VolumeQ = VolumeTpsM - VolumeEF  # Le reste pour la qualité (pour garantir 100%)

    # 2 séances de qualité, on divise le temps de qualité en 2
    TpsSeanceQ = int(VolumeQ / 2) 
    
    nbSeancesEF = nbSeances - 2  # Le reste en séances EF

    return nbSeances, TpsSeanceQ, nbSeancesEF, VolumeQ, VolumeEF

def VolumeAugmentation(VolumeTpsH):
    """Augmente le volume hebdomadaire de 10%."""
    VolumeTpsH *= 1.1  # Augmentation de 10%
    return VolumeTpsH   

def LogSemaines(nbSeances, TpsSeanceQ, TpsSeanceEFCourte, TpsSeanceEFLongue, nbSeancesEF, VolumeQ, VolumeEF, EchauffementVMAQ, CorpsVMAQ, RecupVMAQ, CorpsVMAQeffort, CorpsVMAQrecup, nbfracteffortVMA, EchauffementSeuilQ, CorpsSeuilQ, RecupSeuilQ, CorpsSeuilQeffort, CorpsSeuilQrecup, nbfracteffortSeuil, EchauffementEFCourte, CorpsEFCourte, RecupEFCourte, EchauffementEFLongue, CorpsEFLongue, RecupEFLongue, EchauffementEF, CorpsEF, RecupEF, m, n):
    """Affiche le détail du programme de la semaine."""
    
    print("||||||||||||||||||||||||||||||||||||||||||||||||||||||")
    print(f"La semaine d'entraînement comporte {nbSeances} séances.")
    print("----------------------------------------------------------------------------")
    print(f"Vous avez 2 séances de qualité à effectuer (volume total de qualité : {VolumeQ} minutes) :")
    
    # --- Détail Séance VMA ---
    if n == 1: # n est à 1 après la première exécution de SeanceVMA(n=0)
        # Séance VMA type 1 (fractionné 30s/30s)
        print(f"  - La séance VMA doit durer {TpsSeanceQ} minutes. Elle sera composée de :")
        print(f"    - {EchauffementVMAQ} minutes en Z1 (Échauffement)")
        print(f"    - {CorpsVMAQ} minutes de fractionné : {nbfracteffortVMA} * ({int(CorpsVMAQeffort*60)}s d'effort en Z5 / {int(CorpsVMAQrecup*60)}s de récup en Z1)")
        print(f"    - {RecupVMAQ} minutes en Z1 (Retour au calme)")
    else: # n == 0 (après exécution de SeanceVMA(n=1))
        # Séance VMA type 2 (fractionné plus long)
        print(f"  - La séance VMA doit durer {TpsSeanceQ} minutes. Elle sera composée de :")
        print(f"    - {EchauffementVMAQ} minutes en Z1 (Échauffement)")
        print(f"    - {CorpsVMAQ} minutes de fractionné : {nbfracteffortVMA} * ({CorpsVMAQeffort} min d'effort en Z5 / {CorpsVMAQrecup} min de récup en Z1)")
        print(f"    - {RecupVMAQ} minutes en Z1 (Retour au calme)")

    # --- Détail Séance Seuil ---
    if m == 1: # m est à 1 après la première exécution de SeanceSeuil(m=0)
        # Séance Seuil type 1 (continu)
        print(f"  - La séance Seuil doit durer {TpsSeanceQ} minutes. Elle sera composée de :")
        print(f"    - {EchauffementSeuilQ} minutes en Z1 (Échauffement)")
        print(f"    - {nbfracteffortSeuil} * {CorpsSeuilQeffort} minutes d'effort continu en Z4/Z5")
        print(f"    - {RecupSeuilQ} minutes en Z1 (Retour au calme)")
    else: # m == 0 (après exécution de SeanceSeuil(m=1))
        # Séance Seuil type 2 (fractionné long)
        print(f"  - La séance Seuil doit durer {TpsSeanceQ} minutes. Elle sera composée de :")
        print(f"    - {EchauffementSeuilQ} minutes en Z1 (Échauffement)")
        print(f"    - {CorpsSeuilQ} minutes de fractionné : {nbfracteffortSeuil} * ({CorpsSeuilQeffort} min d'effort en Z4 / {CorpsSeuilQrecup} min de récup en Z1)")
        print(f"    - {RecupSeuilQ} minutes en Z1 (Retour au calme)")
        
    print("----------------------------------------------------------------------------")
    print(f"Vous avez {nbSeancesEF} séance(s) d'endurance fondamentale (EF) à effectuer (volume total EF : {VolumeEF} minutes) :")
    
    if nbSeancesEF == 1:
        print(f"  - La séance d'EF doit durer {VolumeEF} minutes :")
        print(f"    - {EchauffementEF} minutes en Z1 (Échauffement)")
        print(f"    - {CorpsEF} minutes en Z2 (Corps de séance)")
        print(f"    - {RecupEF} minutes en Z1 (Retour au calme)")
    else:
        # Séance Longue
        print(f"  - La séance d'EF longue doit durer {TpsSeanceEFLongue} minutes :")
        print(f"    - {EchauffementEFLongue} minutes en Z1")
        print(f"    - {CorpsEFLongue} minutes en Z2")
        print(f"    - {RecupEFLongue} minutes en Z1")
        
        # Séance(s) Courte(s)
        nb_courtes = nbSeancesEF - 1
        if nb_courtes == 1:
            print(f"  - La séance d'EF courte doit durer {TpsSeanceEFCourte} minutes :")
            print(f"    - {EchauffementEFCourte} minutes en Z1")
            print(f"    - {CorpsEFCourte} minutes en Z2")
            print(f"    - {RecupEFCourte} minutes en Z1")
        else:
            print(f"  - Les {nb_courtes} séances d'EF courtes doivent durer {TpsSeanceEFCourte} minutes chacune :")
            print(f"    - {EchauffementEFCourte} minutes en Z1")
            print(f"    - {CorpsEFCourte} minutes en Z2")
            print(f"    - {RecupEFCourte} minutes en Z1")


def SeanceVMA(TpsSeanceQ, n):
    """Définit le contenu d'une séance VMA, en alternant type 0 et type 1."""
    if n == 0: # Type 1 : VMA Courte (ex: 30s/30s)
        EchauffementQ = int(0.3 * TpsSeanceQ)
        CorpsQ = int(0.4 * TpsSeanceQ)
        RecupQ = TpsSeanceQ - EchauffementQ - CorpsQ # Assure que le total = TpsSeanceQ

        CorpsQeffort = 0.5  # 30 secondes
        CorpsQrecup = 0.5   # 30 secondes
        
        # Calcule le nombre de répétitions 30/30 possible dans le temps 'CorpsQ'
        if (CorpsQeffort + CorpsQrecup) > 0:
            nbfracteffort = int(CorpsQ / (CorpsQeffort + CorpsQrecup))
        else:
            nbfracteffort = 0
            
        n = 1 # On passe au type 1 pour la prochaine fois
        return n, EchauffementQ, CorpsQ, RecupQ, CorpsQeffort, CorpsQrecup, nbfracteffort
        
    else: # Type 2 : VMA Longue (ex: 1x 6min / 6min)
        EchauffementQ = int(0.28 * TpsSeanceQ)
        CorpsQ = int(0.44 * TpsSeanceQ)
        RecupQ = TpsSeanceQ - EchauffementQ - CorpsQ

        nbfracteffort = 1 # Une seule grosse fraction
        CorpsQeffort = int(CorpsQ / 2) # Effort = 50% du corps
        CorpsQrecup = CorpsQ - CorpsQeffort # Récup = 50% du corps

        n = 0 # On passe au type 0 pour la prochaine fois
        return n, EchauffementQ, CorpsQ, RecupQ, CorpsQeffort, CorpsQrecup, nbfracteffort


def SeanceSeuil(TpsSeanceQ, m):
    """Définit le contenu d'une séance Seuil, en alternant type 0 et type 1."""
    if m == 0: # Type 1 : Seuil en continu (ex: 1x 15min)
        EchauffementQ = int(0.3 * TpsSeanceQ)
        CorpsQ = int(0.5 * TpsSeanceQ)
        RecupQ = TpsSeanceQ - EchauffementQ - CorpsQ

        nbfracteffort = 1 # 1 seul bloc
        CorpsQeffort = CorpsQ # Le bloc = tout le corps de séance
        CorpsQrecup = 0

        m = 1 # On passe au type 1 pour la prochaine fois
        return m, EchauffementQ, CorpsQ, RecupQ, CorpsQeffort, CorpsQrecup, nbfracteffort
        
    else: # Type 2 : Seuil fractionné (ex: 3x 5min / 2min)
        EchauffementQ = int(0.25 * TpsSeanceQ)
        CorpsQ = int(0.50 * TpsSeanceQ)
        RecupQ = TpsSeanceQ - EchauffementQ - CorpsQ

        nbfracteffort = 3  # Nombre fixe de fractions (modifiable)
        
        # Temps de récup = 10% du temps total de la séance
        CorpsQrecup = int(0.1 * TpsSeanceQ) 
        if CorpsQrecup < 1: CorpsQrecup = 1 # Minimum 1 min de récup
        
        # Calcule le temps d'effort pour chaque fraction
        total_recup = (nbfracteffort - 1) * CorpsQrecup
        if nbfracteffort > 0:
            CorpsQeffort = int((CorpsQ - total_recup) / nbfracteffort)
        else:
            CorpsQeffort = 0
            
        if CorpsQeffort < 1: CorpsQeffort = 1 # Minimum 1 min d'effort

        m = 0 # On passe au type 0 pour la prochaine fois
        return m, EchauffementQ, CorpsQ, RecupQ, CorpsQeffort, CorpsQrecup, nbfracteffort

def SeanceEF(TpsSeanceEF, nbSeancesEF):   
    """Définit le contenu des séances d'Endurance Fondamentale."""
    
    # Initialisation des variables
    EchauffementEFCourte, CorpsEFCourte, RecupEFCourte = 0, 0, 0
    EchauffementEFLongue, CorpsEFLongue, RecupEFLongue = 0, 0, 0
    TpsSeanceEFCourte, TpsSeanceEFLongue = 0, 0
    EchauffementEF, CorpsEF, RecupEF = 0, 0, 0

    if nbSeancesEF == 1:
        # Une seule séance EF
        TpsSeanceEF_Unique = TpsSeanceEF
        EchauffementEF = int(0.08 * TpsSeanceEF_Unique)
        CorpsEF = int(0.84 * TpsSeanceEF_Unique)
        RecupEF = TpsSeanceEF_Unique - EchauffementEF - CorpsEF
        
    elif nbSeancesEF > 1:
        # S'il y a plusieurs séances EF, on les divise en "longue" et "courte(s)"
        # 50% du volume EF pour la séance longue
        TpsSeanceEFLongue = int(0.5 * TpsSeanceEF) 
        # 50% du volume EF pour la/les séance(s) courte(s)
        VolumeEFCourtes = TpsSeanceEF - TpsSeanceEFLongue 

        # Calcul pour la séance longue
        EchauffementEFLongue = int(0.08 * TpsSeanceEFLongue)
        CorpsEFLongue = int(0.84 * TpsSeanceEFLongue)
        RecupEFLongue = TpsSeanceEFLongue - EchauffementEFLongue - CorpsEFLongue

        # Calcul pour les séances courtes
        nb_courtes = nbSeancesEF - 1
        if nb_courtes > 0:
            # On divise le volume restant par le nombre de séances courtes
            TpsSeanceEFCourte = int(VolumeEFCourtes / nb_courtes) 
        else:
            TpsSeanceEFCourte = VolumeEFCourtes # S'il n'y a qu'une séance "courte" (cas nbSeancesEF=2)

        if TpsSeanceEFCourte > 0:
            EchauffementEFCourte = int(0.08 * TpsSeanceEFCourte)
            CorpsEFCourte = int(0.84 * TpsSeanceEFCourte)
            RecupEFCourte = TpsSeanceEFCourte - EchauffementEFCourte - CorpsEFCourte
        else:
             EchauffementEFCourte, CorpsEFCourte, RecupEFCourte = 0, 0, 0

    return EchauffementEFCourte, CorpsEFCourte, RecupEFCourte, EchauffementEFLongue, CorpsEFLongue, RecupEFLongue, TpsSeanceEFCourte, TpsSeanceEFLongue, EchauffementEF, CorpsEF, RecupEF


# --- Exécution du programme ---
print("")
print(f"La phase generale se fera en {phaseGenerale} semaines.")
print("|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||")

while i < phaseGenerale:

    print(f"\n--- SEMAINE {i + 1} ---")

    # Logique de Périodisation (cycle de 4 semaines)
    numero_semaine_du_cycle = (i % 4) + 1
    
    if numero_semaine_du_cycle == 4:
        # SEMAINE DE RÉCUPÉRATION
        print("Cycle: Semaine de Récupération / Assimilation")
        # On réduit le volume de 40% par rapport à la semaine précédente
        VolumeTps_semaine_precedente = VolumeTps / 1.1 # Annule la dernière augmentation
        VolumeTps = VolumeTps_semaine_precedente * 0.7  
        
    else:
        # SEMAINE DE CHARGE
        print(f"Cycle: Semaine de Charge {numero_semaine_du_cycle}/3")
        # On augmente normalement
        VolumeTps = VolumeAugmentation(VolumeTps)

    print(f"Le volume de temps est de {VolumeTps:.2f} heures")

    # 1. Calculer la structure de la semaine
    data = TPSSeancesSemaine(VolumeTps)
    nbSeances, TpsSeanceQ, nbSeancesEF, VolumeQ, VolumeEF = data
    
    # 2. Définir le contenu des séances de qualité
    # (n et m sont mis à jour à l'intérieur des fonctions)
    dataVMA = SeanceVMA(TpsSeanceQ, n)
    dataSeuil = SeanceSeuil(TpsSeanceQ, m) # Correction : appel à SeanceSeuil
    
    # 3. Définir le contenu des séances EF
    dataEF = SeanceEF(VolumeEF, nbSeancesEF)
    
    # 4. Mettre à jour les compteurs n et m pour la prochaine boucle
    n = dataVMA[0]
    m = dataSeuil[0]
    
    # 5. Afficher le journal de la semaine
    LogSemaines(
        nbSeances, TpsSeanceQ, dataEF[6], dataEF[7], nbSeancesEF, VolumeQ, VolumeEF, # data et dataEF
        dataVMA[1], dataVMA[2], dataVMA[3], dataVMA[4], dataVMA[5], dataVMA[6],     # dataVMA
        dataSeuil[1], dataSeuil[2], dataSeuil[3], dataSeuil[4], dataSeuil[5], dataSeuil[6], # dataSeuil
        dataEF[0], dataEF[1], dataEF[2], dataEF[3], dataEF[4], dataEF[5],             # dataEF (parties courtes/longues)
        dataEF[8], dataEF[9], dataEF[10],                                          # dataEF (partie unique)
        m, n # flags m et n (pour info dans LogSemaines si besoin, même si recalculés avant)
    )

    # 6. Préparer la semaine suivante
    VolumeTps = VolumeAugmentation(VolumeTps)

    # IMPORTANT : Il faut stocker le volume de la dernière semaine de charge
    # pour redémarrer le cycle suivant au bon niveau.
    if numero_semaine_du_cycle == 3:
        volume_fin_de_cycle = VolumeTps
    elif numero_semaine_du_cycle == 4:
        VolumeTps = volume_fin_de_cycle # On repart du volume de S3 pour augmenter en S5
    
    i += 1

    phaseGenerale = phaseGenerale - 1

print("\nProgramme d'entraînement terminé !")