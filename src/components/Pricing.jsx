import './Pricing.css';

const Pricing = () => {
    const plans = [
        {
            name: 'Starter',
            price: '₹499',
            period: '/month',
            description: 'Perfect for small shops and solo entrepreneurs',
            features: [
                'Up to 100 invoices/month',
                'Basic inventory management',
                'Customer database',
                'GST reports',
                'Mobile app access',
                'Email support'
            ],
            popular: false
        },
        {
            name: 'Professional',
            price: '₹1,499',
            period: '/month',
            description: 'For growing businesses with multiple users',
            features: [
                'Unlimited invoices',
                'Advanced inventory tracking',
                'CRM with automation',
                'Financial analytics',
                'Multi-location support',
                '3 user accounts',
                'Priority support',
                'WhatsApp integration'
            ],
            popular: true
        },
        {
            name: 'Enterprise',
            price: 'Custom',
            period: '',
            description: 'Tailored for large teams and enterprises',
            features: [
                'Everything in Professional',
                'Unlimited users',
                'Custom integrations',
                'Dedicated account manager',
                'API access',
                'Advanced security',
                'On-premise deployment',
                'Training & onboarding'
            ],
            popular: false
        }
    ];

    return (
        <section id="pricing" className="pricing">
            <div className="container">
                <div className="section-header">
                    <h2 className="section-title">
                        Simple, <span className="gradient-text">Transparent Pricing</span>
                    </h2>
                    <p className="section-subtitle">
                        Choose the plan that fits your business. No hidden fees. Cancel anytime.
                    </p>
                </div>

                <div className="pricing-grid">
                    {plans.map((plan, index) => (
                        <div
                            key={index}
                            className={`pricing-card ${plan.popular ? 'popular' : ''}`}
                            style={{ animationDelay: `${index * 0.15}s` }}
                        >
                            {plan.popular && <div className="popular-badge">Most Popular</div>}

                            <div className="plan-header">
                                <h3 className="plan-name">{plan.name}</h3>
                                <div className="plan-price">
                                    <span className="price">{plan.price}</span>
                                    <span className="period">{plan.period}</span>
                                </div>
                                <p className="plan-description">{plan.description}</p>
                            </div>

                            <ul className="plan-features">
                                {plan.features.map((feature, idx) => (
                                    <li key={idx}>
                                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                        </svg>
                                        {feature}
                                    </li>
                                ))}
                            </ul>

                            <button className={`btn ${plan.popular ? 'btn-primary' : 'btn-secondary'} btn-large plan-cta`}>
                                {plan.name === 'Enterprise' ? 'Contact Sales' : 'Start Free Trial'}
                            </button>
                        </div>
                    ))}
                </div>

                <div className="pricing-note">
                    <p>All plans include 14-day free trial. No credit card required. 🎉</p>
                </div>
            </div>
        </section>
    );
};

export default Pricing;
