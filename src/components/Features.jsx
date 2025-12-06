import './Features.css';

const Features = () => {
    const features = [
        {
            icon: '📊',
            title: 'Smart Billing & Invoicing',
            description: 'Create GST-compliant invoices in seconds. Auto-calculate taxes and track payments effortlessly.',
            gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
        },
        {
            icon: '📦',
            title: 'Inventory Management',
            description: 'Real-time stock tracking, low-stock alerts, and automated reordering to never run out.',
            gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
        },
        {
            icon: '👥',
            title: 'Customer Management',
            description: 'Build lasting relationships with built-in CRM. Track history, automate follow-ups, boost sales.',
            gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)'
        },
        {
            icon: '💰',
            title: 'Financial Analytics',
            description: 'Get real-time insights into cash flow, expenses, and profitability with beautiful dashboards.',
            gradient: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)'
        },
        {
            icon: '📱',
            title: 'Mobile First',
            description: 'Run your business from anywhere. Full-featured iOS and Android apps with offline support.',
            gradient: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)'
        },
        {
            icon: '🌐',
            title: 'Multi-Language Support',
            description: 'Available in Hindi, English, Tamil, Telugu, Marathi, and more. True vernacular experience.',
            gradient: 'linear-gradient(135deg, #30cfd0 0%, #330867 100%)'
        }
    ];

    return (
        <section id="features" className="features">
            <div className="container">
                <div className="section-header">
                    <h2 className="section-title">Everything You Need to<br /><span className="gradient-text">Grow Your Business</span></h2>
                    <p className="section-subtitle">
                        Powerful features designed specifically for Indian MSMEs. Simple to use, yet powerful enough to scale.
                    </p>
                </div>

                <div className="features-grid">
                    {features.map((feature, index) => (
                        <div
                            key={index}
                            className="feature-card"
                            style={{ animationDelay: `${index * 0.1}s` }}
                        >
                            <div className="feature-icon" style={{ background: feature.gradient }}>
                                <span>{feature.icon}</span>
                            </div>
                            <h3 className="feature-title">{feature.title}</h3>
                            <p className="feature-description">{feature.description}</p>
                        </div>
                    ))}
                </div>

                <div className="features-cta">
                    <button className="btn btn-primary btn-large">
                        Explore All Features
                    </button>
                </div>
            </div>
        </section>
    );
};

export default Features;
