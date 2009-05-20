package org.jasig.portal.spring.locator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.portal.channel.IChannelPublishingService;
import org.jasig.portal.spring.PortalApplicationContextLocator;
import org.springframework.context.ApplicationContext;

public class ChannelPublishingServiceLocator extends AbstractBeanLocator<IChannelPublishingService> {
    public static final String BEAN_NAME = "channelPublishingService";
    
    private static final Log LOG = LogFactory.getLog(ChannelPublishingServiceLocator.class);
    private static AbstractBeanLocator<IChannelPublishingService> locatorInstance;

    public static IChannelPublishingService getIChannelPublishingService() {
        AbstractBeanLocator<IChannelPublishingService> locator = locatorInstance;
        if (locator == null) {
            LOG.info("Looking up bean '" + BEAN_NAME + "' in ApplicationContext due to context not yet being initialized");
            final ApplicationContext applicationContext = PortalApplicationContextLocator.getApplicationContext();
            applicationContext.getBean(ChannelPublishingServiceLocator.class.getName());
            
            locator = locatorInstance;
            if (locator == null) {
                LOG.warn("Instance of '" + BEAN_NAME + "' still null after portal application context has been initialized");
                return (IChannelPublishingService)applicationContext.getBean(BEAN_NAME, IChannelPublishingService.class);
            }
        }
        
        return locator.getInstance();
    }

    public ChannelPublishingServiceLocator(IChannelPublishingService instance) {
        super(instance, IChannelPublishingService.class);
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.spring.locator.AbstractBeanLocator#getLocator()
     */
    @Override
    protected AbstractBeanLocator<IChannelPublishingService> getLocator() {
        return locatorInstance;
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.spring.locator.AbstractBeanLocator#setLocator(org.jasig.portal.spring.locator.AbstractBeanLocator)
     */
    @Override
    protected void setLocator(AbstractBeanLocator<IChannelPublishingService> locator) {
        locatorInstance = locator;
    }
}