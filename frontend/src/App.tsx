import React, { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { Sidebar, TabId } from './components/Sidebar';
import { AuthModal } from './components/AuthModal';
import { DashboardOverview } from './pages/DashboardOverview';
import { ServicesView } from './pages/ServicesView';
import { IncidentsView } from './pages/IncidentsView';
import { AnalyticsView } from './pages/AnalyticsView';
import { AlertsView } from './pages/AlertsView';
import { TeamSettingsView } from './pages/TeamSettingsView';
import { PlatformAdminView } from './pages/PlatformAdminView';
import { ApiClient } from './services/api';

const AppContent: React.FC = () => {
  const { isAuthenticated, isLoading, activeOrg } = useAuth();
  const [activeTab, setActiveTab] = useState<TabId>('overview');
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [openIncidentsCount, setOpenIncidentsCount] = useState(0);
  const [analyticsServiceId, setAnalyticsServiceId] = useState<string | undefined>();

  useEffect(() => {
    if (activeOrg?.id) {
      ApiClient.getIncidents('OPEN')
        .then((incs) => setOpenIncidentsCount(incs.length))
        .catch(() => {});
    }
  }, [activeOrg?.id, activeTab]);

  // If initial load and not authenticated, automatically open Auth / Demo Modal
  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      setIsAuthModalOpen(true);
    }
  }, [isLoading, isAuthenticated]);

  const handleSelectServiceForAnalytics = (serviceId: string) => {
    setAnalyticsServiceId(serviceId);
    setActiveTab('analytics');
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      <Navbar onOpenAuth={() => setIsAuthModalOpen(true)} />

      <div className="flex-1 flex overflow-hidden">
        <Sidebar
          activeTab={activeTab}
          onSelectTab={setActiveTab}
          openIncidentsCount={openIncidentsCount}
        />

        <main className="flex-1 overflow-y-auto bg-slate-950">
          {activeTab === 'overview' && (
            <DashboardOverview
              onNavigateToServices={() => setActiveTab('services')}
              onNavigateToIncidents={() => setActiveTab('incidents')}
            />
          )}

          {activeTab === 'services' && (
            <ServicesView onSelectServiceForAnalytics={handleSelectServiceForAnalytics} />
          )}

          {activeTab === 'incidents' && <IncidentsView />}

          {activeTab === 'analytics' && <AnalyticsView initialServiceId={analyticsServiceId} />}

          {activeTab === 'alerts' && <AlertsView />}

          {activeTab === 'team' && <TeamSettingsView />}

          {activeTab === 'admin' && <PlatformAdminView />}
        </main>
      </div>

      <AuthModal isOpen={isAuthModalOpen} onClose={() => setIsAuthModalOpen(false)} />
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
};

export default App;
