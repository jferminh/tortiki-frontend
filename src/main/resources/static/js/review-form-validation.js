(function () {
    'use strict';

    // Cible le formulaire de soumission d'avis, identifié par son action POST /reviews.
    const form = document.querySelector('form[action$="/reviews"]');
    if (!form) {
        return;
    }

    const ratingInputs = form.querySelectorAll('input[name="rating"]');
    const ratingFieldset = form.querySelector('fieldset');

    /**
     * Crée ou récupère le conteneur d'erreur client dédié à la note.
     * Distinct du message d'erreur serveur Thymeleaf pour éviter tout conflit
     * d'affichage entre validation serveur (BindingResult) et validation JS.
     */
    function getRatingErrorContainer() {
        let container = ratingFieldset.querySelector('.js-rating-error');
        if (!container) {
            container = document.createElement('div');
            container.className = 'text-danger small mt-2 js-rating-error';
            container.setAttribute('role', 'alert');
            ratingFieldset.appendChild(container);
        }
        return container;
    }

    function isRatingSelected() {
        return Array.prototype.some.call(ratingInputs, function (input) {
            return input.checked;
        });
    }

    form.addEventListener('submit', function (event) {
        if (!isRatingSelected()) {
            event.preventDefault();
            getRatingErrorContainer().textContent =
                'Merci de sélectionner une note avant de publier votre avis.';
            ratingInputs[0].focus();
        }
    });

    ratingInputs.forEach(function (input) {
        input.addEventListener('change', function () {
            const errorContainer = ratingFieldset.querySelector('.js-rating-error');
            if (errorContainer) {
                errorContainer.textContent = '';
            }
        });
    });
})();