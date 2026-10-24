import { AICoach } from "@/components/AICoach";
import { ClipShowcase } from "@/components/ClipShowcase";
import { Faq } from "@/components/Faq";
import { FinalCta } from "@/components/FinalCta";
import { Footer } from "@/components/Footer";
import { Header } from "@/components/Header";
import { Hero } from "@/components/Hero";
import { JsonLd } from "@/components/JsonLd";
import { Platform } from "@/components/Platform";
import { Pricing } from "@/components/Pricing";
import { Security } from "@/components/Security";
import { Solutions } from "@/components/Solutions";
import { Trust } from "@/components/Trust";
import { WaitlistForm } from "@/components/WaitlistForm";
import { faq, site } from "@/lib/content";

// Structured data states only what is true: no ratings, no prices, no contact details that do not exist.
const organization = {
  "@context": "https://schema.org",
  "@type": "Organization",
  name: site.name,
  url: site.url,
  logo: `${site.url}/brand/quantiva-icon-512.png`,
  description: site.description,
};

const product = {
  "@context": "https://schema.org",
  "@type": "Product",
  name: site.name,
  description: site.description,
  category: site.category,
  brand: { "@type": "Brand", name: site.name },
  image: `${site.url}/brand/quantiva-icon-512.png`,
};

const faqPage = {
  "@context": "https://schema.org",
  "@type": "FAQPage",
  mainEntity: faq.items.map((f) => ({
    "@type": "Question",
    name: f.q,
    acceptedAnswer: { "@type": "Answer", text: f.a },
  })),
};

export default function Home() {
  return (
    <>
      <JsonLd data={organization} />
      <JsonLd data={product} />
      <JsonLd data={faqPage} />
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[60] focus:rounded-brand focus:bg-white focus:px-4 focus:py-2"
      >
        Skip to content
      </a>
      <Header />
      <main id="main">
        <Hero />
        <Trust />
        <ClipShowcase />
        <Platform />
        <AICoach />
        <Solutions />
        <Pricing />
        <Security />
        <Faq />
        <WaitlistForm />
        <FinalCta />
      </main>
      <Footer />
    </>
  );
}
