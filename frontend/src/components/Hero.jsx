import './Hero.css';
import useActivityTracker from '../hooks/useActivityTracker';

const Hero = () => {
    const { trackClick } = useActivityTracker();

    const handleStartTrial = () => {
        trackClick('hero_start_trial_click', { buttonText: 'Start Free Trial', section: 'hero' });
    };

    const handleWatchDemo = () => {
        trackClick('hero_watch_demo_click', { buttonText: 'Watch Demo', section: 'hero' });
    };

    return (
        <section className="hero">
            <div className="hero-background">
                <div className="gradient-orb orb-1"></div>
                <div className="gradient-orb orb-2"></div>
                <div className="hero-grid"></div>
            </div>

            <div className="container hero-content">
                <div className="hero-text animate-fade-in-up">
                    <h1 className="hero-title">
                        Business Management
                        <span className="gradient-text">Made Simple</span>
                    </h1>
                    <p className="hero-subtitle">
                        Empower your MSME with Suvidha - the all-in-one SaaS platform designed for Indian businesses.
                        Manage billing, inventory, customers, and more with ease.
                    </p>
                    <div className="hero-cta">
                        <button className="btn btn-primary btn-large" onClick={handleStartTrial}>
                            Start Free Trial
                            <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
                            </svg>
                        </button>
                        <button className="btn btn-secondary btn-large" onClick={handleWatchDemo}>
                            Watch Demo
                        </button>
                    </div>
                    <div className="hero-stats">
                        <div className="stat">
                            <span className="stat-number">10,000+</span>
                            <span className="stat-label">Businesses Trust Us</span>
                        </div>
                        <div className="stat">
                            <span className="stat-number">₹500M+</span>
                            <span className="stat-label">Transactions Processed</span>
                        </div>
                        <div className="stat">
                            <span className="stat-number">99.9%</span>
                            <span className="stat-label">Uptime</span>
                        </div>
                    </div>
                </div>

                <div className="hero-image animate-fade-in">
                    <div className="dashboard-mockup">
                        <div className="mockup-header">
                            <div className="mockup-dots">
                                <span></span><span></span><span></span>
                            </div>
                            <div className="mockup-title">Suvidha Dashboard</div>
                        </div>
                        <div className="mockup-content">
                            <div className="mockup-card card-1">
                                <div className="card-header">Today's Sales</div>
                                <div className="card-value">₹45,680</div>
                                <div className="card-trend positive">↑ 12.5%</div>
                            </div>
                            <div className="mockup-card card-2">
                                <div className="card-header">Pending Payments</div>
                                <div className="card-value">₹12,340</div>
                                <div className="card-trend">3 invoices</div>
                            </div>
                            <div className="mockup-card card-3">
                                <div className="card-header">Inventory Status</div>
                                <div className="card-value">98%</div>
                                <div className="card-trend positive">↑ Stock healthy</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div className="scroll-indicator">
                <div className="mouse">
                    <div className="wheel"></div>
                </div>
                <p>Scroll to explore</p>
            </div>
        </section>
    );
};

export default Hero;
