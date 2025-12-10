import './Footer.css';
import useActivityTracker from '../hooks/useActivityTracker';

const Footer = () => {
    const { trackClick } = useActivityTracker();

    const handleFooterLinkClick = (linkName, category) => {
        trackClick('footer_link_click', { linkName, category, section: 'footer' });
    };

    const handleSocialClick = (platform) => {
        trackClick('footer_social_click', { platform, section: 'footer' });
    };

    return (
        <footer className="footer">
            <div className="footer-top">
                <div className="container">
                    <div className="footer-grid">
                        <div className="footer-brand">
                            <div className="logo">
                                <div className="logo-icon"></div>
                                <div className="logo-text">
                                    <span className="logo-name">SUVIDHA</span>
                                    <span className="logo-tagline">सुविधा</span>
                                </div>
                            </div>
                            <p className="footer-description">
                                Empowering Indian MSMEs with simple, powerful business management tools.
                                Built for India, by Indians.
                            </p>
                            <div className="social-links">
                                <a href="#" aria-label="Facebook" onClick={() => handleSocialClick('Facebook')}>FB</a>
                                <a href="#" aria-label="Twitter" onClick={() => handleSocialClick('Twitter')}>TW</a>
                                <a href="#" aria-label="LinkedIn" onClick={() => handleSocialClick('LinkedIn')}>IN</a>
                                <a href="#" aria-label="Instagram" onClick={() => handleSocialClick('Instagram')}>IG</a>
                            </div>
                        </div>

                        <div className="footer-links">
                            <h4>Product</h4>
                            <ul>
                                <li><a href="#features" onClick={() => handleFooterLinkClick('Features', 'Product')}>Features</a></li>
                                <li><a href="#pricing" onClick={() => handleFooterLinkClick('Pricing', 'Product')}>Pricing</a></li>
                                <li><a href="#" onClick={() => handleFooterLinkClick('Integrations', 'Product')}>Integrations</a></li>
                                <li><a href="#" onClick={() => handleFooterLinkClick('Mobile App', 'Product')}>Mobile App</a></li>
                            </ul>
                        </div>

                        <div className="footer-links">
                            <h4>Solutions</h4>
                            <ul>
                                <li><a href="#" onClick={() => handleFooterLinkClick('Retail', 'Solutions')}>Retail</a></li>
                                <li><a href="#" onClick={() => handleFooterLinkClick('Manufacturing', 'Solutions')}>Manufacturing</a></li>
                                <li><a href="#" onClick={() => handleFooterLinkClick('Services', 'Solutions')}>Services</a></li>
                                <li><a href="#" onClick={() => handleFooterLinkClick('Wholesale', 'Solutions')}>Wholesale</a></li>
                            </ul>
                        </div>

                        <div className="footer-links">
                            <h4>Company</h4>
                            <ul>
                                <li><a href="#" onClick={() => handleFooterLinkClick('About Us', 'Company')}>About Us</a></li>
                                <li><a href="#" onClick={() => handleFooterLinkClick('Careers', 'Company')}>Careers</a></li>
                                <li><a href="#" onClick={() => handleFooterLinkClick('Blog', 'Company')}>Blog</a></li>
                                <li><a href="#" onClick={() => handleFooterLinkClick('Contact', 'Company')}>Contact</a></li>
                            </ul>
                        </div>

                        <div className="footer-links">
                            <h4>Support</h4>
                            <ul>
                                <li><a href="#" onClick={() => handleFooterLinkClick('Help Center', 'Support')}>Help Center</a></li>
                                <li><a href="#" onClick={() => handleFooterLinkClick('Documentation', 'Support')}>Documentation</a></li>
                                <li><a href="#" onClick={() => handleFooterLinkClick('Privacy Policy', 'Support')}>Privacy Policy</a></li>
                                <li><a href="#" onClick={() => handleFooterLinkClick('Terms of Service', 'Support')}>Terms of Service</a></li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>

            <div className="footer-bottom">
                <div className="container">
                    <p>&copy; 2024 Suvidha. All rights reserved. Made with ❤️ in India</p>
                </div>
            </div>
        </footer>
    );
};

export default Footer;
