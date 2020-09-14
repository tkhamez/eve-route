import React from 'react';
import { Link, Typography } from "@material-ui/core";
import { Trans, useTranslation } from "react-i18next";

export default function HowItWorks() {
  const { t } = useTranslation();

  return (
    <div className="grid-spacing-2-wrapper modal">
      <Typography variant="body2">
        {t('howItWorks.gates-alliance')}
      </Typography>
      <br/>
      <Typography variant="body2">
        <Trans i18nKey="howItWorks.esi-results">
          %<Link href="https://github.com/esi/esi-issues/issues/108#issuecomment-600691564"
                 target="_blank" rel="noopener noreferrer">%</Link>%
        </Trans>
      </Typography>
      <br/>
      <Typography variant="body2">
        {t('howItWorks.unknown-gates')}
      </Typography>
      <br/>
      <Typography variant="body2">
        {t('howItWorks.wormholes')}
      </Typography>
      <br/>
      <Typography variant="body2">
        {t('howItWorks.several-routes')}
      </Typography>
    </div>
  )
}
