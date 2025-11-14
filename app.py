# -*- coding: utf-8 -*-
from flask import Flask, render_template, request, jsonify
import io
import sys

app = Flask(__name__)

@app.template_filter('format_time')
def format_time(minutes):
    """Convertit les minutes en format heures:minutes si > 59 minutes."""
    if minutes >= 60:
        hours = int(minutes // 60)
        mins = int(minutes % 60)
        return f"{hours}h{mins:02d}min"
    else:
        return f"{int(minutes)}min"

class TrainingProgramGenerator:
    """Classe pour générer un programme d'entraînement"""
    
    def __init__(self, volume_tps, nb_semaine_prog, objectif):
        self.VolumeTps = volume_tps
        self.NBsemaineProg = nb_semaine_prog
        self.objectif = objectif
        self.n = 0  # Compteur VMA
        self.m = 0  # Compteur Seuil
        self.results = []
        
        # Calculs initiaux
        Kbase = 10
        K = Kbase + (objectif * 0.5)
        Sd = objectif / (21 + K)
        self.phaseGenerale = round(((40 + (30 * Sd))/100) * nb_semaine_prog)
        self.phaseAffutage = round(0.1 * nb_semaine_prog)
        self.phaseSpecifique = nb_semaine_prog - self.phaseGenerale - self.phaseAffutage
        
    def TPSSeancesSemaine(self, VolumeTpsH):
        """Calcule la répartition du temps d'entraînement pour la semaine."""
        VolumeTpsM = int(VolumeTpsH * 60)
        nbSeances = int(VolumeTpsH + 3)
        VolumeEF = int(0.8 * VolumeTpsM)
        VolumeQ = VolumeTpsM - VolumeEF
        TpsSeanceQ = int(VolumeQ / 2)
        nbSeancesEF = nbSeances - 2
        return nbSeances, TpsSeanceQ, nbSeancesEF, VolumeQ, VolumeEF

    def VolumeAugmentation(self, VolumeTpsH):
        """Augmente le volume hebdomadaire de 10%."""
        return VolumeTpsH * 1.1

    def SeanceVMA(self, TpsSeanceQ, n):
        """Définit le contenu d'une séance VMA."""
        if n == 0:
            EchauffementQ = int(0.3 * TpsSeanceQ)
            CorpsQ = int(0.4 * TpsSeanceQ)
            RecupQ = TpsSeanceQ - EchauffementQ - CorpsQ
            CorpsQeffort = 0.5
            CorpsQrecup = 0.5
            if (CorpsQeffort + CorpsQrecup) > 0:
                nbfracteffort = int(CorpsQ / (CorpsQeffort + CorpsQrecup))
            else:
                nbfracteffort = 0
            n = 1
        else:
            EchauffementQ = int(0.28 * TpsSeanceQ)
            CorpsQ = int(0.44 * TpsSeanceQ)
            RecupQ = TpsSeanceQ - EchauffementQ - CorpsQ
            nbfracteffort = 1
            CorpsQeffort = int(CorpsQ / 2)
            CorpsQrecup = CorpsQ - CorpsQeffort
            n = 0
        return n, EchauffementQ, CorpsQ, RecupQ, CorpsQeffort, CorpsQrecup, nbfracteffort

    def SeanceSeuil(self, TpsSeanceQ, m):
        """Définit le contenu d'une séance Seuil."""
        if m == 0:
            EchauffementQ = int(0.3 * TpsSeanceQ)
            CorpsQ = int(0.5 * TpsSeanceQ)
            RecupQ = TpsSeanceQ - EchauffementQ - CorpsQ
            nbfracteffort = 1
            CorpsQeffort = CorpsQ
            CorpsQrecup = 0
            m = 1
        else:
            EchauffementQ = int(0.25 * TpsSeanceQ)
            CorpsQ = int(0.50 * TpsSeanceQ)
            RecupQ = TpsSeanceQ - EchauffementQ - CorpsQ
            nbfracteffort = 3
            CorpsQrecup = int(0.1 * TpsSeanceQ)
            if CorpsQrecup < 1: CorpsQrecup = 1
            total_recup = (nbfracteffort - 1) * CorpsQrecup
            if nbfracteffort > 0:
                CorpsQeffort = int((CorpsQ - total_recup) / nbfracteffort)
            else:
                CorpsQeffort = 0
            if CorpsQeffort < 1: CorpsQeffort = 1
            m = 0
        return m, EchauffementQ, CorpsQ, RecupQ, CorpsQeffort, CorpsQrecup, nbfracteffort

    def SeanceEF(self, TpsSeanceEF, nbSeancesEF):
        """Définit le contenu des séances d'Endurance Fondamentale."""
        EchauffementEFCourte, CorpsEFCourte, RecupEFCourte = 0, 0, 0
        EchauffementEFLongue, CorpsEFLongue, RecupEFLongue = 0, 0, 0
        TpsSeanceEFCourte, TpsSeanceEFLongue = 0, 0
        EchauffementEF, CorpsEF, RecupEF = 0, 0, 0

        if nbSeancesEF == 1:
            TpsSeanceEF_Unique = TpsSeanceEF
            EchauffementEF = int(0.08 * TpsSeanceEF_Unique)
            CorpsEF = int(0.84 * TpsSeanceEF_Unique)
            RecupEF = TpsSeanceEF_Unique - EchauffementEF - CorpsEF
        elif nbSeancesEF > 1:
            TpsSeanceEFLongue = int(0.5 * TpsSeanceEF)
            VolumeEFCourtes = TpsSeanceEF - TpsSeanceEFLongue
            EchauffementEFLongue = int(0.08 * TpsSeanceEFLongue)
            CorpsEFLongue = int(0.84 * TpsSeanceEFLongue)
            RecupEFLongue = TpsSeanceEFLongue - EchauffementEFLongue - CorpsEFLongue
            nb_courtes = nbSeancesEF - 1
            if nb_courtes > 0:
                TpsSeanceEFCourte = int(VolumeEFCourtes / nb_courtes)
            else:
                TpsSeanceEFCourte = VolumeEFCourtes
            if TpsSeanceEFCourte > 0:
                EchauffementEFCourte = int(0.08 * TpsSeanceEFCourte)
                CorpsEFCourte = int(0.84 * TpsSeanceEFCourte)
                RecupEFCourte = TpsSeanceEFCourte - EchauffementEFCourte - CorpsEFCourte

        return EchauffementEFCourte, CorpsEFCourte, RecupEFCourte, EchauffementEFLongue, CorpsEFLongue, RecupEFLongue, TpsSeanceEFCourte, TpsSeanceEFLongue, EchauffementEF, CorpsEF, RecupEF

    def generate_program(self):
        """Génère le programme complet."""
        i = 0
        phaseGenerale = self.phaseGenerale
        VolumeTps = self.VolumeTps
        volume_fin_de_cycle = VolumeTps
        
        while i < self.phaseGenerale:
            week_data = {
                'numero': i + 1,
                'cycle': '',
                'volume': 0,
                'seances': []
            }
            
            numero_semaine_du_cycle = (i % 4) + 1
            
            if numero_semaine_du_cycle == 4:
                week_data['cycle'] = "Semaine de Récupération / Assimilation"
                VolumeTps_semaine_precedente = VolumeTps / 1.1
                VolumeTps = VolumeTps_semaine_precedente * 0.7
            else:
                week_data['cycle'] = f"Semaine de Charge {numero_semaine_du_cycle}/3"
                VolumeTps = self.VolumeAugmentation(VolumeTps)
            
            week_data['volume'] = round(VolumeTps, 2)
            
            data = self.TPSSeancesSemaine(VolumeTps)
            nbSeances, TpsSeanceQ, nbSeancesEF, VolumeQ, VolumeEF = data
            
            dataVMA = self.SeanceVMA(TpsSeanceQ, self.n)
            dataSeuil = self.SeanceSeuil(TpsSeanceQ, self.m)
            dataEF = self.SeanceEF(VolumeEF, nbSeancesEF)
            
            # Séance VMA
            vma_detail = {
                'type': 'VMA',
                'duree': TpsSeanceQ,
                'echauffement': dataVMA[1],
                'corps': dataVMA[2],
                'recup': dataVMA[3],
                'effort': dataVMA[4],
                'recup_effort': dataVMA[5],
                'nb_fractions': dataVMA[6],
                'is_short': self.n == 0  # Avant mise à jour
            }
            week_data['seances'].append(vma_detail)
            
            # Séance Seuil
            seuil_detail = {
                'type': 'Seuil',
                'duree': TpsSeanceQ,
                'echauffement': dataSeuil[1],
                'corps': dataSeuil[2],
                'recup': dataSeuil[3],
                'effort': dataSeuil[4],
                'recup_effort': dataSeuil[5],
                'nb_fractions': dataSeuil[6],
                'is_continuous': self.m == 0  # Avant mise à jour
            }
            week_data['seances'].append(seuil_detail)
            
            # Séances EF
            if nbSeancesEF == 1:
                ef_detail = {
                    'type': 'EF_unique',
                    'duree': VolumeEF,
                    'echauffement': dataEF[8],
                    'corps': dataEF[9],
                    'recup': dataEF[10]
                }
                week_data['seances'].append(ef_detail)
            else:
                # Longue
                ef_longue = {
                    'type': 'EF_longue',
                    'duree': dataEF[7],
                    'echauffement': dataEF[3],
                    'corps': dataEF[4],
                    'recup': dataEF[5]
                }
                week_data['seances'].append(ef_longue)
                
                # Courtes
                nb_courtes = nbSeancesEF - 1
                for _ in range(nb_courtes):
                    ef_courte = {
                        'type': 'EF_courte',
                        'duree': dataEF[6],
                        'echauffement': dataEF[0],
                        'corps': dataEF[1],
                        'recup': dataEF[2]
                    }
                    week_data['seances'].append(ef_courte)
            
            self.n = dataVMA[0]
            self.m = dataSeuil[0]
            self.results.append(week_data)
            
            # Gérer le volume pour la semaine suivante
            if numero_semaine_du_cycle == 3:
                # Après une semaine 3, stocker le volume actuel augmenté pour redémarrer après la récup
                volume_fin_de_cycle = self.VolumeAugmentation(VolumeTps)
            elif numero_semaine_du_cycle == 4:
                # Après une semaine de récup, reprendre au niveau de fin de cycle
                VolumeTps = volume_fin_de_cycle
            else:
                # Pour les semaines 1 et 2, continuer normalement
                VolumeTps = self.VolumeAugmentation(VolumeTps)
            
            i += 1
        
        return self.results


@app.route('/')
def index():
    """Page d'accueil avec le formulaire."""
    return render_template('index.html')


@app.route('/generate', methods=['POST'])
def generate():
    """Génère le programme d'entraînement."""
    try:
        volume_tps = float(request.form.get('volume_tps'))
        nb_semaines = int(request.form.get('nb_semaines'))
        objectif = int(request.form.get('objectif'))
        
        generator = TrainingProgramGenerator(volume_tps, nb_semaines, objectif)
        results = generator.generate_program()
        
        return render_template('results.html', 
                             weeks=results, 
                             phase_generale=generator.phaseGenerale,
                             objectif=objectif,
                             nb_semaines=nb_semaines)
    except Exception as e:
        return f"Erreur: {str(e)}", 400


if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)
