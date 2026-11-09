import { useEffect, useState } from 'react';
import { backupDue, snoozeBackup } from '~/lib/backup-reminder';
import './data.css';

/** A one-line prompt on the home and progress pages when a backup is overdue. */
export default function BackupReminder({ dataUrl }: { dataUrl: string }) {
  const [due, setDue] = useState(false);
  useEffect(() => setDue(backupDue()), []);
  if (!due) return null;
  return (
    <p className="dt-reminder" role="status">
      <span>
        Your progress exists only in this browser and hasn't been backed up for a while.{' '}
        <a href={dataUrl}>Back it up</a>
      </span>
      <button
        type="button"
        className="btn"
        onClick={() => {
          snoozeBackup();
          setDue(false);
        }}
      >
        Not now
      </button>
    </p>
  );
}
