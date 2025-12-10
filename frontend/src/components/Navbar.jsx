import { useState, useEffect } from 'react';
import './Navbar.css';
import useActivityTracker from '../hooks/useActivityTracker';

const Navbar = () => {
    const [isScrolled, setIsScrolled] = useState(false);
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
    const { trackClick } = useActivityTracker();

    useEffect(() => {
        const handleScroll = () => {
            setIsScrolled(window.scrollY > 50);
        };
        window.addEventListener('scroll', handleScroll);
        return () => window.removeEventListener('scroll', handleScroll);
    }, []);

    const handleNavClick = (linkName) => {
        trackClick('navbar_link_click', { linkName, section: 'navbar' });
    };

    const handleLogin = () => {
        trackClick('navbar_login_click', { buttonText: 'Login', section: 'navbar' });
    };

    const handleGetStarted = () => {
        trackClick('navbar_get_started_click', { buttonText: 'Get Started', section: 'navbar' });
    };

    return (
        <nav className={`navbar ${isScrolled ? 'scrolled' : ''}`}>
            <div className="container navbar-content">
                <div className="navbar-brand">
                    <div className="logo">
                        <div className="logo-icon"></div>
                        <div className="logo-text">
                            <span className="logo-name">SUVIDHA</span>
                            <span className="logo-tagline">सुविधा</span>
                        </div>
                    </div>
                </div>

                <ul className={`navbar-links ${isMobileMenuOpen ? 'mobile-open' : ''}`}>
                    <li><a href="#features" onClick={() => handleNavClick('Features')}>Features</a></li>
                    <li><a href="#solutions" onClick={() => handleNavClick('Solutions')}>Solutions</a></li>
                    <li><a href="#pricing" onClick={() => handleNavClick('Pricing')}>Pricing</a></li>
                    <li><a href="#testimonials" onClick={() => handleNavClick('Testimonials')}>Testimonials</a></li>
                </ul>

                <div className="navbar-actions">
                    <button className="btn-login" onClick={handleLogin}>Login</button>
                    <button className="btn btn-primary" onClick={handleGetStarted}>Get Started</button>
                </div>

                <button
                    className="mobile-menu-toggle"
                    onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                    aria-label="Toggle menu"
                >
                    <span></span>
                    <span></span>
                    <span></span>
                </button>
            </div>
        </nav>
    );
};

export default Navbar;
