/**
 * Modal Accessibility and Keyboard Handling
 * Provides ESC key support, focus trapping, and keyboard navigation for modals
 */

(function() {
    'use strict';

    // Modal configurations
    const modals = {
        addressModal: {
            element: null,
            triggerElements: [],
            firstFocusableElement: null,
            lastFocusableElement: null
        },
        paymentModal: {
            element: null,
            triggerElements: [],
            firstFocusableElement: null,
            lastFocusableElement: null
        },
        avatarViewModal: {
            element: null,
            triggerElements: [],
            firstFocusableElement: null,
            lastFocusableElement: null
        }
    };

    // Focusable element selector
    const focusableSelector = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';

    /**
     * Initialize modals on page load
     */
    function initModals() {
        Object.keys(modals).forEach(modalKey => {
            const modal = modals[modalKey];
            modal.element = document.getElementById(modalKey);
            
            if (!modal.element) return;

            // Add event listeners to modal
            modal.element.addEventListener('keydown', handleModalKeydown);
            modal.element.addEventListener('click', handleModalBackdropClick);

            // Update focusable elements when modal opens
            const observer = new MutationObserver(() => {
                updateFocusableElements(modal);
            });
            observer.observe(modal.element, { 
                attributes: true, 
                attributeFilter: ['class'],
                subtree: true
            });
        });

        // Set up button triggers for modals
        setupModalTriggers();
    }

    /**
     * Setup triggers for opening modals (buttons/links with data-modal attribute)
     */
    function setupModalTriggers() {
        document.querySelectorAll('[data-modal]').forEach(trigger => {
            trigger.addEventListener('click', (e) => {
                e.preventDefault();
                const modalId = trigger.getAttribute('data-modal');
                const modal = document.getElementById(modalId);
                if (modal) {
                    openModal(modal, trigger);
                }
            });
        });
    }

    /**
     * Open modal and set focus
     */
    function openModal(modal, triggerElement) {
        modal.classList.remove('hidden');
        modal.setAttribute('aria-hidden', 'false');
        updateFocusableElements(modals[modal.id]);
        
        // Focus first input or button in modal
        const firstInput = modal.querySelector('input, button, select, textarea');
        if (firstInput) {
            setTimeout(() => firstInput.focus(), 0);
        }
    }

    /**
     * Close modal and return focus to trigger
     */
    function closeModal(modalElement, returnFocusTo) {
        if (!modalElement) return;
        
        modalElement.classList.add('hidden');
        modalElement.setAttribute('aria-hidden', 'true');
        
        if (returnFocusTo && typeof returnFocusTo.focus === 'function') {
            returnFocusTo.focus();
        }
    }

    /**
     * Update focusable elements within modal
     */
    function updateFocusableElements(modal) {
        if (!modal.element || modal.element.classList.contains('hidden')) return;

        const focusableElements = modal.element.querySelectorAll(focusableSelector);
        if (focusableElements.length === 0) return;

        modal.firstFocusableElement = focusableElements[0];
        modal.lastFocusableElement = focusableElements[focusableElements.length - 1];
    }

    /**
     * Handle keyboard events in modals
     */
    function handleModalKeydown(e) {
        if (this.classList.contains('hidden')) return;

        const modal = modals[this.id];
        if (!modal) return;

        // ESC key closes modal
        if (e.key === 'Escape') {
            e.preventDefault();
            closeModal(this);
            return;
        }

        // TAB key handling for focus trap
        if (e.key === 'Tab') {
            if (!modal.firstFocusableElement || !modal.lastFocusableElement) {
                e.preventDefault();
                return;
            }

            // Shift+Tab on first element - move to last
            if (e.shiftKey && document.activeElement === modal.firstFocusableElement) {
                e.preventDefault();
                modal.lastFocusableElement.focus();
                return;
            }

            // Tab on last element - move to first
            if (!e.shiftKey && document.activeElement === modal.lastFocusableElement) {
                e.preventDefault();
                modal.firstFocusableElement.focus();
                return;
            }
        }

        // Enter key on buttons outside form context
        if (e.key === 'Enter' && e.target.tagName !== 'TEXTAREA') {
            if (e.target.tagName === 'BUTTON' || 
                (e.target.tagName === 'A' && e.target.getAttribute('data-action'))) {
                // Allow default button/link behavior
                return;
            }
        }
    }

    /**
     * Close modal when clicking on backdrop
     */
    function handleModalBackdropClick(e) {
        if (e.target === this && !this.classList.contains('hidden')) {
            closeModal(this);
        }
    }

    /**
     * Handle onclick="document.getElementById('modalId').classList.add('hidden')" style close buttons
     * This is for backward compatibility with existing close buttons
     */
    function patchCloseButtons() {
        document.querySelectorAll('[onclick*="classList.add"]').forEach(btn => {
            const originalOnclick = btn.getAttribute('onclick');
            if (originalOnclick && originalOnclick.includes('hidden')) {
                btn.addEventListener('click', function(e) {
                    // Execute original onclick
                    eval(originalOnclick);
                });
            }
        });
    }

    /**
     * Add ARIA attributes to modals for accessibility
     */
    function addAriaAttributes() {
        Object.values(modals).forEach(modal => {
            if (modal.element) {
                modal.element.setAttribute('role', 'dialog');
                modal.element.setAttribute('aria-modal', 'true');
                modal.element.setAttribute('aria-hidden', 'true');
                modal.element.setAttribute('tabindex', '-1');
            }
        });
    }

    /**
     * Initialize everything when DOM is ready
     */
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            addAriaAttributes();
            initModals();
            patchCloseButtons();
        });
    } else {
        addAriaAttributes();
        initModals();
        patchCloseButtons();
    }

    // Expose global function for manual modal closing (backward compatibility)
    window.closeModal = function(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            closeModal(modal);
        }
    };

    // Expose global function for manual modal opening (backward compatibility)
    window.openModalElement = function(modalId, triggerElement) {
        const modal = document.getElementById(modalId);
        if (modal) {
            openModal(modal, triggerElement);
        }
    };

})();
