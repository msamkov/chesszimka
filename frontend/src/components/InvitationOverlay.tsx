import { useNetwork } from '../context/NetworkContext';

/** Глобальный оверлей, показываемый поверх любой страницы, когда другой игрок пригласил нас сыграть. */
export function InvitationOverlay() {
  const { incomingInvitation, acceptInvite, declineInvite } = useNetwork();

  if (!incomingInvitation) {
    return null;
  }

  return (
    <div className="overlay-backdrop">
      <div className="overlay-card">
        <p className="invite-name">{incomingInvitation.fromName}</p>
        <p>хочет сыграть с вами в шахматы</p>
        <div className="button-row">
          <button className="primary" onClick={acceptInvite}>
            Принять
          </button>
          <button className="secondary" onClick={declineInvite}>
            Отклонить
          </button>
        </div>
      </div>
    </div>
  );
}
